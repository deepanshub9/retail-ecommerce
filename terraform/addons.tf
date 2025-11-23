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
  # AWS GATEWAY API CONTROLLER - Modern Load Balancing and Routing
  # =============================================================================
  enable_aws_gateway_api_controller = true
  aws_gateway_api_controller = {
    most_recent = true
    namespace   = "aws-application-networking-system"
  }

  # =============================================================================
  # OPTIONAL: MONITORING STACK
  # =============================================================================
  # Uncomment below to enable monitoring (increases costs)

  # enable_kube_prometheus_stack = var.enable_monitoring
  # kube_prometheus_stack = {
  #   most_recent = true
  #   namespace   = "monitoring"
  # }

  depends_on = [module.retail_app_eks]
}

# =============================================================================
# GATEWAY API CRDs AND CONFIGURATION
# =============================================================================

# Wait for Gateway API Controller to be ready
resource "time_sleep" "wait_for_gateway_controller" {
  create_duration = "60s"
  depends_on      = [module.eks_addons]
}

# Create GatewayClass for AWS VPC Lattice
resource "kubectl_manifest" "gateway_class" {
  yaml_body = <<-YAML
    apiVersion: gateway.networking.k8s.io/v1
    kind: GatewayClass
    metadata:
      name: amazon-vpc-lattice
    spec:
      controllerName: application-networking.k8s.aws/gateway-api-controller
  YAML

  depends_on = [time_sleep.wait_for_gateway_controller]
}

# Create Gateway for retail store applications
resource "kubectl_manifest" "retail_store_gateway" {
  yaml_body = <<-YAML
    apiVersion: gateway.networking.k8s.io/v1
    kind: Gateway
    metadata:
      name: retail-store-gateway
      namespace: retail-store
      annotations:
        application-networking.k8s.aws/lattice-vpc-association: "true"
    spec:
      gatewayClassName: amazon-vpc-lattice
      listeners:
      - name: http
        protocol: HTTP
        port: 80
        allowedRoutes:
          namespaces:
            from: Same
  YAML

  depends_on = [
    kubectl_manifest.gateway_class,
    kubectl_manifest.argocd_apps
  ]
}

# Create HTTPRoute for UI service
resource "kubectl_manifest" "ui_http_route" {
  yaml_body = <<-YAML
    apiVersion: gateway.networking.k8s.io/v1
    kind: HTTPRoute
    metadata:
      name: ui-route
      namespace: retail-store
    spec:
      parentRefs:
      - name: retail-store-gateway
        namespace: retail-store
      rules:
      - matches:
        - path:
            type: PathPrefix
            value: /
        backendRefs:
        - name: retail-store-ui
          port: 80
  YAML

  depends_on = [kubectl_manifest.retail_store_gateway]
}
