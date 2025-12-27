# =============================================================================
# ARGOCD INSTALLATION AND CONFIGURATION
# =============================================================================

# Wait for the cluster and add-ons to be ready
resource "time_sleep" "wait_for_cluster" {
  create_duration = "30s"
  depends_on = [
    module.retail_app_eks,
    module.eks_addons
  ]
}

# =============================================================================
# ARGOCD HELM INSTALLATION
# =============================================================================

resource "helm_release" "argocd" {
  name             = "argocd"
  namespace        = var.argocd_namespace
  create_namespace = true

  repository = "https://argoproj.github.io/argo-helm"
  chart      = "argo-cd"
  version    = var.argocd_chart_version

  # Cleanup behavior: do not force updates; only clean up on failed installs/upgrades (no special destroy behavior)
  force_update     = false
  cleanup_on_fail  = true
  
  # Wait for resources to be ready
  wait          = true
  wait_for_jobs = true
  timeout       = 600

  # ArgoCD configuration values
  values = [
    yamlencode({
      # Server configuration
      server = {
        service = {
          type = "ClusterIP"
        }
        ingress = {
          enabled = false  # We'll use port-forward for access
        }
        # Enable insecure mode for easier local access
        extraArgs = [
          "--insecure"
        ]
      }
      
      # Controller configuration
      controller = {
        resources = {
          requests = {
            cpu    = "100m"
            memory = "128Mi"
          }
          limits = {
            cpu    = "500m"
            memory = "512Mi"
          }
        }
      }
      
      # Repo server configuration
      repoServer = {
        resources = {
          requests = {
            cpu    = "50m"
            memory = "64Mi"
          }
          limits = {
            cpu    = "200m"
            memory = "256Mi"
          }
        }
      }
      
      # Redis configuration
      redis = {
        resources = {
          requests = {
            cpu    = "50m"
            memory = "64Mi"
          }
          limits = {
            cpu    = "200m"
            memory = "128Mi"
          }
        }
      }
    })
  ]

  depends_on = [time_sleep.wait_for_cluster]
}

# =============================================================================
# ARGOCD CONFIGURATION
# =============================================================================

resource "kubectl_manifest" "argocd_projects" {
  for_each   = fileset("${path.module}/../argocd/projects", "*.yaml")
  yaml_body  = file("${path.module}/../argocd/projects/${each.value}")
  
  # Force delete on destroy
  force_new = false
  
  depends_on = [helm_release.argocd]
}

resource "kubectl_manifest" "argocd_apps" {
  for_each   = fileset("${path.module}/../argocd/applications", "*.yaml")
  yaml_body  = file("${path.module}/../argocd/applications/${each.value}")
  
  # Force delete on destroy
  force_new = false
  
  depends_on = [kubectl_manifest.argocd_projects]
}

# =============================================================================
# CLEANUP HOOK FOR ARGOCD APPLICATIONS
# This ensures ArgoCD applications are cleaned up before the namespace is deleted
# =============================================================================

resource "null_resource" "cleanup_argocd_apps" {
  triggers = {
    cluster_name = module.retail_app_eks.cluster_name
    region       = var.aws_region
    namespace    = var.argocd_namespace
  }

  provisioner "local-exec" {
    when    = destroy
    command = <<-EOT
      echo "Cleaning up ArgoCD applications..."
      
      # Update kubeconfig
      if ! aws eks update-kubeconfig --name ${self.triggers.cluster_name} --region ${self.triggers.region} 2>/dev/null; then
        echo "Warning: Failed to update kubeconfig for cluster '${self.triggers.cluster_name}' in region '${self.triggers.region}'."
        echo "AWS credentials or EKS access may be missing. Skipping ArgoCD Kubernetes resource cleanup."
        # Do not fail the Terraform destroy; cleanup is best-effort.
        exit 0
      fi
      
      # Delete all ArgoCD applications (this will delete the deployed resources)
      kubectl delete applications.argoproj.io --all -n ${self.triggers.namespace} --ignore-not-found --timeout=120s 2>/dev/null || true
      
      # Delete ArgoCD projects
      kubectl delete appprojects.argoproj.io --all -n ${self.triggers.namespace} --ignore-not-found --timeout=60s 2>/dev/null || true
      
      # Wait for applications to be fully cleaned up with polling
      echo "Waiting for ArgoCD applications to be cleaned up..."
      MAX_RETRIES=20
      SLEEP_SECONDS=30
      i=1
      while [ "$i" -le "$MAX_RETRIES" ]; do
        REMAINING_APPS=$(kubectl get applications.argoproj.io -n ${self.triggers.namespace} --no-headers 2>/dev/null | wc -l | tr -d ' ')
        if [ -z "$REMAINING_APPS" ]; then
          REMAINING_APPS=0
        fi
        
        if [ "$REMAINING_APPS" -eq 0 ] 2>/dev/null; then
          echo "All ArgoCD applications have been cleaned up."
          break
        fi
        
        echo "ArgoCD applications still present (${REMAINING_APPS}). Waiting (${i}/${MAX_RETRIES})..."
        i=$((i + 1))
        sleep "$SLEEP_SECONDS"
      done
      
      if [ "$REMAINING_APPS" -ne 0 ] 2>/dev/null; then
        echo "Warning: Some ArgoCD applications may still exist after waiting ${MAX_RETRIES} * ${SLEEP_SECONDS}s."
      fi
      
      echo "ArgoCD cleanup complete!"
    EOT
    on_failure = continue
  }

  depends_on = [
    kubectl_manifest.argocd_apps,
    kubectl_manifest.argocd_projects,
    helm_release.argocd
  ]
}
