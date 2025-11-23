# =============================================================================
# SECURITY GROUPS AND RULES
# =============================================================================

# Data source for VPC Lattice managed prefix list
data "aws_ec2_managed_prefix_list" "vpc_lattice" {
  filter {
    name   = "prefix-list-name"
    values = ["com.amazonaws.${var.aws_region}.vpc-lattice"]
  }
}

# Security group for VPC Lattice traffic
resource "aws_security_group" "vpc_lattice" {
  name        = "${local.cluster_name}-vpc-lattice-sg"
  description = "Security group for VPC Lattice traffic to EKS cluster"
  vpc_id      = module.vpc.vpc_id

  tags = merge(
    local.common_tags,
    {
      Name = "${local.cluster_name}-vpc-lattice-sg"
    }
  )
}

# Allow all traffic from VPC Lattice managed prefix list
resource "aws_security_group_rule" "vpc_lattice_ingress" {
  description       = "Allow traffic from VPC Lattice"
  type              = "ingress"
  from_port         = 0
  to_port           = 65535
  protocol          = "tcp"
  prefix_list_ids   = [data.aws_ec2_managed_prefix_list.vpc_lattice.id]
  security_group_id = aws_security_group.vpc_lattice.id
}

# Allow all outbound traffic from VPC Lattice security group
resource "aws_security_group_rule" "vpc_lattice_egress" {
  description       = "Allow all outbound traffic"
  type              = "egress"
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = aws_security_group.vpc_lattice.id
}

# Allow VPC Lattice traffic to EKS nodes
resource "aws_security_group_rule" "nodes_vpc_lattice" {
  description              = "Allow VPC Lattice traffic to EKS nodes"
  type                     = "ingress"
  from_port                = 0
  to_port                  = 65535
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.vpc_lattice.id
  security_group_id        = module.retail_app_eks.cluster_security_group_id
}

# Allow HTTP traffic within VPC for internal services
resource "aws_security_group_rule" "internal_http" {
  description       = "Allow HTTP traffic within VPC"
  type              = "ingress"
  from_port         = 80
  to_port           = 80
  protocol          = "tcp"
  cidr_blocks       = [module.vpc.vpc_cidr_block]
  security_group_id = module.retail_app_eks.cluster_security_group_id
}

# Allow HTTPS traffic within VPC for internal services
resource "aws_security_group_rule" "internal_https" {
  description       = "Allow HTTPS traffic within VPC"
  type              = "ingress"
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = [module.vpc.vpc_cidr_block]
  security_group_id = module.retail_app_eks.cluster_security_group_id
}

# Allow application port 8080 within VPC
resource "aws_security_group_rule" "internal_app_port" {
  description       = "Allow application traffic on port 8080 within VPC"
  type              = "ingress"
  from_port         = 8080
  to_port           = 8080
  protocol          = "tcp"
  cidr_blocks       = [module.vpc.vpc_cidr_block]
  security_group_id = module.retail_app_eks.cluster_security_group_id
}

# Allow NodePort range for services (if needed)
resource "aws_security_group_rule" "nodeport_access" {
  description       = "Allow NodePort access within VPC"
  type              = "ingress"
  from_port         = 30000
  to_port           = 32767
  protocol          = "tcp"
  cidr_blocks       = [module.vpc.vpc_cidr_block]
  security_group_id = module.retail_app_eks.cluster_security_group_id
}
