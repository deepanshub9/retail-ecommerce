# =============================================================================
# OUTPUT VALUES
# =============================================================================

# =============================================================================
# CLUSTER INFORMATION
# =============================================================================

output "cluster_name" {
  description = "Name of the EKS cluster (with unique suffix)"
  value       = module.retail_app_eks.cluster_name
}

output "cluster_name_base" {
  description = "Base cluster name without suffix"
  value       = var.cluster_name
}

output "cluster_name_suffix" {
  description = "Random suffix added to cluster name"
  value       = random_string.suffix.result
}

output "cluster_endpoint" {
  description = "Endpoint for EKS control plane"
  value       = module.retail_app_eks.cluster_endpoint
}

output "cluster_version" {
  description = "The Kubernetes version for the EKS cluster"
  value       = module.retail_app_eks.cluster_version
}

output "cluster_security_group_id" {
  description = "Security group ID attached to the EKS cluster"
  value       = module.retail_app_eks.cluster_security_group_id
}

output "cluster_oidc_issuer_url" {
  description = "The URL on the EKS cluster for the OpenID Connect identity provider"
  value       = module.retail_app_eks.cluster_oidc_issuer_url
}

# =============================================================================
# NETWORK INFORMATION
# =============================================================================

output "vpc_id" {
  description = "ID of the VPC where the cluster is deployed"
  value       = module.vpc.vpc_id
}

output "vpc_cidr_block" {
  description = "CIDR block of the VPC"
  value       = module.vpc.vpc_cidr_block
}

output "private_subnets" {
  description = "List of IDs of private subnets"
  value       = module.vpc.private_subnets
}

output "public_subnets" {
  description = "List of IDs of public subnets"
  value       = module.vpc.public_subnets
}

# =============================================================================
# ACCESS INFORMATION
# =============================================================================

output "configure_kubectl" {
  description = "Configure kubectl: make sure you're logged in with the correct AWS profile and run the following command to update your kubeconfig"
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.retail_app_eks.cluster_name}"
}

output "argocd_namespace" {
  description = "Namespace where ArgoCD is installed"
  value       = var.argocd_namespace
}

output "argocd_server_port_forward" {
  description = "Command to port-forward to ArgoCD server"
  value       = "kubectl port-forward svc/argocd-server -n ${var.argocd_namespace} 8080:443"
}

output "argocd_admin_password" {
  description = "Command to get ArgoCD admin password"
  value       = "kubectl -n ${var.argocd_namespace} get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d"
  sensitive   = true
}

# =============================================================================
# GATEWAY API ACCESS
# =============================================================================

output "gateway_status" {
  description = "Command to get Gateway status"
  value       = "kubectl get gateway retail-store-gateway -n retail-store -o yaml"
}

output "gateway_dns" {
  description = "Command to get Gateway DNS endpoint"
  value       = "kubectl get gateway retail-store-gateway -n retail-store -o jsonpath='{.status.addresses[0].value}'"
}

output "http_routes" {
  description = "Command to list all HTTPRoutes"
  value       = "kubectl get httproute -n retail-store"
}

output "retail_store_url" {
  description = "Command to get the retail store application URL"
  value       = "kubectl get gateway retail-store-gateway -n retail-store -o jsonpath='{.status.addresses[0].value}'"
}

output "vpc_lattice_services" {
  description = "Command to list VPC Lattice services"
  value       = "aws vpc-lattice list-services --region ${var.aws_region}"
}

# =============================================================================
# USEFUL COMMANDS
# =============================================================================

output "useful_commands" {
  description = "Useful commands for managing the cluster"
  value = {
    get_nodes            = "kubectl get nodes"
    get_pods_all         = "kubectl get pods -A"
    get_retail_store     = "kubectl get pods -n retail-store"
    argocd_apps          = "kubectl get applications -n ${var.argocd_namespace}"
    gateway_status       = "kubectl get gateway -A"
    gateway_classes      = "kubectl get gatewayclass"
    http_routes          = "kubectl get httproute -A"
    describe_cluster     = "kubectl cluster-info"
    vpc_lattice_services = "aws vpc-lattice list-services --region ${var.aws_region}"
    vpc_lattice_networks = "aws vpc-lattice list-service-networks --region ${var.aws_region}"
  }
}
