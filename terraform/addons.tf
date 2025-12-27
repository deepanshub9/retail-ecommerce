# =============================================================================
# EKS ADD-ONS AND EXTENSIONS
# =============================================================================

module "eks_addons" {
  source  = "aws-ia/eks-blueprints-addons/aws"
  version = "~> 1.0"

  # Cluster information
  cluster_name      = module.retail_app_eks.cluster_name
  cluster_endpoint  = module.retail_app_eks.cluster_endpoint
  cluster_version   = module.retail_app_eks.cluster_version
  oidc_provider_arn = module.retail_app_eks.oidc_provider_arn

  # =============================================================================
  # CERT-MANAGER - SSL Certificate Management
  # =============================================================================
  enable_cert_manager = true
  cert_manager = {
    most_recent = true
    namespace   = "cert-manager"
  }

  # =============================================================================
  # NGINX INGRESS CONTROLLER - Load Balancing and Routing
  # =============================================================================
  enable_ingress_nginx = true
  ingress_nginx = {
    most_recent = true
    namespace   = "ingress-nginx"

    # Basic configuration
    set = [
      {
        name  = "controller.service.type"
        value = "LoadBalancer"
      },
      {
        name  = "controller.service.externalTrafficPolicy"
        value = "Local"
      },
      {
        name  = "controller.resources.requests.cpu"
        value = "100m"
      },
      {
        name  = "controller.resources.requests.memory"
        value = "128Mi"
      },
      {
        name  = "controller.resources.limits.cpu"
        value = "200m"
      },
      {
        name  = "controller.resources.limits.memory"
        value = "256Mi"
      }
    ]

    # AWS Load Balancer specific annotations
    set_sensitive = [
      {
        name  = "controller.service.annotations.service\\.beta\\.kubernetes\\.io/aws-load-balancer-scheme"
        value = "internet-facing"
      },
      {
        name  = "controller.service.annotations.service\\.beta\\.kubernetes\\.io/aws-load-balancer-type"
        value = "nlb"
      },
      {
        name  = "controller.service.annotations.service\\.beta\\.kubernetes\\.io/aws-load-balancer-nlb-target-type"
        value = "instance"
      },
      {
        name  = "controller.service.annotations.service\\.beta\\.kubernetes\\.io/aws-load-balancer-health-check-path"
        value = "/healthz"
      },
      {
        name  = "controller.service.annotations.service\\.beta\\.kubernetes\\.io/aws-load-balancer-health-check-port"
        value = "10254"
      },
      {
        name  = "controller.service.annotations.service\\.beta\\.kubernetes\\.io/aws-load-balancer-health-check-protocol"
        value = "HTTP"
      }
    ]
  }

  # =============================================================================
  # OPTIONAL: MONITORING STACK
  # =============================================================================
  # Uncomment below to enable monitoring (increases costs)

  enable_kube_prometheus_stack = var.enable_monitoring
  kube_prometheus_stack = {
    most_recent = true
    namespace   = "monitoring"
  }

  # =============================================================================
  # OPTIONAL: AWS LOAD BALANCER CONTROLLER
  # =============================================================================
  enable_aws_load_balancer_controller = true
  aws_load_balancer_controller = {
    most_recent = true
    namespace   = "kube-system"
    set = [
      {
        name  = "vpcId"
        value = module.vpc.vpc_id
      }
    ]
  }

  depends_on = [module.retail_app_eks]
}

# =============================================================================
# CLEANUP HOOK FOR EKS ADDONS
# This ensures that Kubernetes services (LoadBalancers) are cleaned up
# before the EKS cluster and VPC are destroyed.
# =============================================================================

resource "null_resource" "cleanup_k8s_services" {
  triggers = {
    cluster_name = module.retail_app_eks.cluster_name
    region       = var.aws_region
    vpc_id       = module.vpc.vpc_id
  }

  provisioner "local-exec" {
    when    = destroy
    command = <<-EOT
      echo "Cleaning up Kubernetes LoadBalancer services..."
      
      # Update kubeconfig
      aws eks update-kubeconfig --name ${self.triggers.cluster_name} --region ${self.triggers.region} 2>/dev/null
      if [ $? -ne 0 ]; then
        echo "WARNING: Failed to update kubeconfig for cluster '${self.triggers.cluster_name}' in region '${self.triggers.region}'."
        echo "         This is often caused by missing or invalid AWS credentials during 'terraform destroy'."
        echo "         Skipping Kubernetes cleanup of LoadBalancer services; AWS load balancers may remain."
        # Exit successfully so Terraform can continue destroying other resources.
        exit 0
      fi
      
      # Delete ingress-nginx namespace first (releases NLB)
      kubectl delete namespace ingress-nginx --ignore-not-found --timeout=120s 2>/dev/null || true
      
      # Delete all LoadBalancer type services across all namespaces
      for ns in $(kubectl get namespaces -o jsonpath='{.items[*].metadata.name}' 2>/dev/null); do
        kubectl delete svc -n "$ns" --field-selector spec.type=LoadBalancer --ignore-not-found --timeout=60s 2>/dev/null || true
      done
      
      # Wait for load balancers to be fully deleted
      echo "Waiting for AWS Load Balancers to be deleted..."
      for i in {1..12}; do
        LB_COUNT=$(aws elbv2 describe-load-balancers --region ${self.triggers.region} --query "length(LoadBalancers[?VpcId=='${self.triggers.vpc_id}'])" --output text 2>/dev/null || echo "0")
        if [ "$LB_COUNT" = "0" ]; then
          echo "All load balancers deleted."
          break
        fi
        echo "Waiting for $LB_COUNT load balancer(s) to be deleted... (attempt $i/12)"
        sleep 15
      done
      
      echo "Kubernetes cleanup complete!"
    EOT
    on_failure = continue
  }

  depends_on = [module.eks_addons]
}
