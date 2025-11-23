# =============================================================================
# GATEWAY API CRDs INSTALLATION
# =============================================================================

# Install Gateway API CRDs using kubectl apply
resource "null_resource" "install_gateway_api_crds" {
  provisioner "local-exec" {
    command = <<-EOT
      aws eks update-kubeconfig --region ${var.aws_region} --name ${module.retail_app_eks.cluster_name} --kubeconfig ${path.module}/.kubeconfig
      export KUBECONFIG=${path.module}/.kubeconfig
      kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.0.0/standard-install.yaml
    EOT

    when = create
  }

  provisioner "local-exec" {
    command = <<-EOT
      aws eks update-kubeconfig --region ${var.aws_region} --name ${module.retail_app_eks.cluster_name} --kubeconfig ${path.module}/.kubeconfig
      export KUBECONFIG=${path.module}/.kubeconfig
      kubectl delete -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.0.0/standard-install.yaml || true
      rm -f ${path.module}/.kubeconfig
    EOT

    when = destroy
  }

  depends_on = [module.retail_app_eks]

  triggers = {
    cluster_name = module.retail_app_eks.cluster_name
    region       = var.aws_region
  }
}

# Wait for CRDs to be established in the cluster
resource "time_sleep" "wait_for_gateway_crds" {
  create_duration = "45s"

  depends_on = [null_resource.install_gateway_api_crds]
}

# Verify Gateway API CRDs are installed
resource "null_resource" "verify_gateway_api_crds" {
  provisioner "local-exec" {
    command = <<-EOT
      aws eks update-kubeconfig --region ${var.aws_region} --name ${module.retail_app_eks.cluster_name} --kubeconfig ${path.module}/.kubeconfig
      export KUBECONFIG=${path.module}/.kubeconfig
      
      echo "Verifying Gateway API CRDs..."
      kubectl get crd gatewayclasses.gateway.networking.k8s.io || exit 1
      kubectl get crd gateways.gateway.networking.k8s.io || exit 1
      kubectl get crd httproutes.gateway.networking.k8s.io || exit 1
      
      echo "Gateway API CRDs verified successfully!"
    EOT
  }

  depends_on = [time_sleep.wait_for_gateway_crds]
}
