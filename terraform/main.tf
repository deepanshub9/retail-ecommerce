# =============================================================================
# MAIN INFRASTRUCTURE RESOURCES
# =============================================================================

# =============================================================================
# VPC CONFIGURATION
# =============================================================================

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.0"

  name = "${var.cluster_name}-vpc"
  cidr = var.vpc_cidr

  azs             = local.azs
  public_subnets  = local.public_subnets
  private_subnets = local.private_subnets

  # NAT Gateway configuration
  enable_nat_gateway = true
  single_nat_gateway = var.enable_single_nat_gateway

  # Internet Gateway
  create_igw = true

  # DNS configuration
  enable_dns_hostnames = true
  enable_dns_support   = true

  # Manage default resources for better control
  manage_default_network_acl    = true
  default_network_acl_tags      = { Name = "${var.cluster_name}-default-nacl" }
  manage_default_route_table    = true
  default_route_table_tags      = { Name = "${var.cluster_name}-default-rt" }
  manage_default_security_group = true
  default_security_group_tags   = { Name = "${var.cluster_name}-default-sg" }

  # Apply Kubernetes-specific tags to subnets
  public_subnet_tags  = merge(local.common_tags, local.public_subnet_tags)
  private_subnet_tags = merge(local.common_tags, local.private_subnet_tags)

  tags = local.common_tags
}

# =============================================================================
# CLEANUP RESOURCES BEFORE VPC DELETION
# This null_resource ensures that AWS Load Balancers created by the 
# AWS Load Balancer Controller are deleted before the VPC is destroyed.
# Without this, VPC deletion will hang waiting for ENIs to be released.
# =============================================================================

resource "null_resource" "cleanup_load_balancers" {
  # This runs during terraform destroy, before the VPC is deleted
  triggers = {
    vpc_id     = module.vpc.vpc_id
    region     = var.aws_region
    cluster    = local.cluster_name
  }

  provisioner "local-exec" {
    when    = destroy
    command = <<-EOT
      echo "Cleaning up AWS Load Balancers in VPC ${self.triggers.vpc_id}..."
      
      # Delete all load balancers in the VPC
      for lb in $(aws elbv2 describe-load-balancers --region ${self.triggers.region} --query "LoadBalancers[?VpcId=='${self.triggers.vpc_id}'].LoadBalancerArn" --output text 2>/dev/null); do
        echo "Deleting load balancer: $lb"
        aws elbv2 delete-load-balancer --load-balancer-arn "$lb" --region ${self.triggers.region} 2>/dev/null || true
      done
      
      # Wait for load balancers to be fully deleted with polling
      echo "Waiting for load balancers to be deleted..."
      for i in {1..20}; do
        LB_COUNT=$(aws elbv2 describe-load-balancers --region ${self.triggers.region} --query "length(LoadBalancers[?VpcId=='${self.triggers.vpc_id}'])" --output text 2>/dev/null || echo "0")
        if [ "$LB_COUNT" = "0" ]; then
          echo "All load balancers deleted."
          break
        fi
        echo "Waiting for $LB_COUNT load balancer(s) to be deleted... (attempt $i/20)"
        sleep 15
      done
      
      # Delete all target groups in the VPC
      for tg in $(aws elbv2 describe-target-groups --region ${self.triggers.region} --query "TargetGroups[?VpcId=='${self.triggers.vpc_id}'].TargetGroupArn" --output text 2>/dev/null); do
        echo "Deleting target group: $tg"
        aws elbv2 delete-target-group --target-group-arn "$tg" --region ${self.triggers.region} 2>/dev/null || true
      done
      
      # Wait for ENIs to be released with polling
      echo "Waiting for ENIs to be released..."
      for i in {1..12}; do
        ENI_COUNT=$(aws ec2 describe-network-interfaces --region ${self.triggers.region} --filters "Name=vpc-id,Values=${self.triggers.vpc_id}" "Name=status,Values=in-use" --query "length(NetworkInterfaces)" --output text 2>/dev/null || echo "0")
        if [ "$ENI_COUNT" = "0" ]; then
          echo "All ENIs released."
          break
        fi
        echo "Waiting for $ENI_COUNT ENI(s) to be released... (attempt $i/12)"
        sleep 10
      done
      
      echo "Cleanup complete!"
    EOT
    on_failure = continue
  }

  depends_on = [module.vpc]
}

# =============================================================================
# EKS CLUSTER CONFIGURATION
# =============================================================================

module "retail_app_eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.31"

  # Basic cluster configuration
  cluster_name    = local.cluster_name
  cluster_version = var.kubernetes_version

  # Cluster access configuration
  cluster_endpoint_public_access           = true
  cluster_endpoint_private_access          = true
  enable_cluster_creator_admin_permissions = true

  # EKS Auto Mode configuration - simplified node management
  cluster_compute_config = {
    enabled    = true
    node_pools = ["general-purpose"]
  }

  # Network configuration
  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  # KMS configuration to avoid conflicts
  create_kms_key = true
  kms_key_description = "EKS cluster ${local.cluster_name} encryption key"
  kms_key_deletion_window_in_days = 7
  
  # Cluster logging (optional - can be expensive)
  cluster_enabled_log_types = []

  tags = local.common_tags
}
