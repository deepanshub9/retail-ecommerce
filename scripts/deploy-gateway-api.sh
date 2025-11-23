#!/bin/bash

# =============================================================================
# Gateway API Deployment Script
# =============================================================================
# This script deploys the retail store application using Kubernetes Gateway API
# with AWS VPC Lattice instead of NGINX Ingress Controller.
#
# Prerequisites:
# - AWS CLI configured with appropriate credentials
# - kubectl installed and configured
# - Terraform installed
# - Helm installed
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Functions
print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

check_prerequisites() {
    print_header "Checking Prerequisites"
    
    # Check AWS CLI
    if command -v aws &> /dev/null; then
        print_success "AWS CLI is installed"
        aws --version
    else
        print_error "AWS CLI is not installed. Please install it first."
        exit 1
    fi
    
    # Check kubectl
    if command -v kubectl &> /dev/null; then
        print_success "kubectl is installed"
        kubectl version --client --short 2>/dev/null || kubectl version --client
    else
        print_error "kubectl is not installed. Please install it first."
        exit 1
    fi
    
    # Check Terraform
    if command -v terraform &> /dev/null; then
        print_success "Terraform is installed"
        terraform version | head -n 1
    else
        print_error "Terraform is not installed. Please install it first."
        exit 1
    fi
    
    # Check Helm
    if command -v helm &> /dev/null; then
        print_success "Helm is installed"
        helm version --short
    else
        print_error "Helm is not installed. Please install it first."
        exit 1
    fi
    
    echo ""
}

deploy_infrastructure() {
    print_header "Deploying Infrastructure with Terraform"
    
    cd terraform
    
    print_info "Initializing Terraform..."
    terraform init -upgrade
    
    print_info "Planning Terraform deployment..."
    terraform plan -out=tfplan
    
    print_info "Applying Terraform configuration..."
    terraform apply tfplan
    
    print_success "Infrastructure deployed successfully"
    
    # Get cluster name
    CLUSTER_NAME=$(terraform output -raw cluster_name)
    AWS_REGION=$(terraform output -json | jq -r '.useful_commands.value.vpc_lattice_services' | grep -oP '(?<=--region )\S+')
    
    cd ..
    echo ""
}

configure_kubectl() {
    print_header "Configuring kubectl"
    
    print_info "Updating kubeconfig for cluster: $CLUSTER_NAME"
    aws eks update-kubeconfig --name "$CLUSTER_NAME" --region "$AWS_REGION"
    
    print_success "kubectl configured successfully"
    
    # Verify connection
    print_info "Verifying cluster connection..."
    kubectl cluster-info
    
    echo ""
}

wait_for_gateway_api() {
    print_header "Waiting for Gateway API Resources"
    
    print_info "Waiting for Gateway API Controller to be ready..."
    kubectl wait --for=condition=available --timeout=300s \
        deployment -l app.kubernetes.io/name=gateway-api-controller \
        -n aws-application-networking-system 2>/dev/null || {
        print_info "Gateway API Controller deployment not found yet, waiting..."
        sleep 30
    }
    
    print_info "Checking GatewayClass..."
    for i in {1..30}; do
        if kubectl get gatewayclass amazon-vpc-lattice &>/dev/null; then
            print_success "GatewayClass is ready"
            break
        fi
        echo -n "."
        sleep 10
    done
    
    echo ""
}

verify_gateway() {
    print_header "Verifying Gateway Deployment"
    
    print_info "Waiting for Gateway to be ready..."
    for i in {1..60}; do
        STATUS=$(kubectl get gateway retail-store-gateway -n retail-store -o jsonpath='{.status.conditions[?(@.type=="Programmed")].status}' 2>/dev/null || echo "")
        if [ "$STATUS" = "True" ]; then
            print_success "Gateway is ready and programmed"
            break
        fi
        if [ $i -eq 60 ]; then
            print_error "Gateway did not become ready within 10 minutes"
            kubectl describe gateway retail-store-gateway -n retail-store
            exit 1
        fi
        echo -n "."
        sleep 10
    done
    
    # Get Gateway address
    GATEWAY_URL=$(kubectl get gateway retail-store-gateway -n retail-store \
        -o jsonpath='{.status.addresses[0].value}' 2>/dev/null || echo "")
    
    if [ -n "$GATEWAY_URL" ]; then
        print_success "Gateway DNS: $GATEWAY_URL"
    else
        print_info "Gateway DNS not available yet (this is normal, it may take a few minutes)"
    fi
    
    echo ""
}

verify_httproutes() {
    print_header "Verifying HTTPRoutes"
    
    print_info "Checking HTTPRoute status..."
    kubectl get httproute -n retail-store
    
    # Check if routes are attached to gateway
    ROUTE_COUNT=$(kubectl get httproute -n retail-store -o json | jq '.items | length')
    print_info "Found $ROUTE_COUNT HTTPRoute(s)"
    
    echo ""
}

display_access_info() {
    print_header "Access Information"
    
    echo -e "${GREEN}Deployment Complete!${NC}"
    echo ""
    echo "Cluster Name: $CLUSTER_NAME"
    echo "AWS Region: $AWS_REGION"
    echo ""
    
    # ArgoCD access
    echo -e "${YELLOW}ArgoCD Access:${NC}"
    echo "1. Get admin password:"
    echo "   kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d"
    echo ""
    echo "2. Port forward to ArgoCD:"
    echo "   kubectl port-forward svc/argocd-server -n argocd 8080:443"
    echo ""
    echo "3. Access ArgoCD UI:"
    echo "   https://localhost:8080"
    echo "   Username: admin"
    echo ""
    
    # Application access
    echo -e "${YELLOW}Application Access:${NC}"
    if [ -n "$GATEWAY_URL" ]; then
        echo "Retail Store URL: http://$GATEWAY_URL"
    else
        echo "Gateway URL is being provisioned. Check status with:"
        echo "   kubectl get gateway retail-store-gateway -n retail-store"
    fi
    echo ""
    
    # Useful commands
    echo -e "${YELLOW}Useful Commands:${NC}"
    echo "Check Gateway status:"
    echo "   kubectl get gateway -A"
    echo ""
    echo "Check HTTPRoutes:"
    echo "   kubectl get httproute -n retail-store"
    echo ""
    echo "Check application pods:"
    echo "   kubectl get pods -n retail-store"
    echo ""
    echo "View VPC Lattice services:"
    echo "   aws vpc-lattice list-services --region $AWS_REGION"
    echo ""
    echo "Get Gateway DNS endpoint:"
    echo "   kubectl get gateway retail-store-gateway -n retail-store -o jsonpath='{.status.addresses[0].value}'"
    echo ""
}

# Main execution
main() {
    print_header "Gateway API Deployment for Retail Store"
    echo ""
    
    check_prerequisites
    deploy_infrastructure
    configure_kubectl
    wait_for_gateway_api
    
    # Wait a bit for ArgoCD to deploy applications
    print_info "Waiting for ArgoCD to deploy applications (60 seconds)..."
    sleep 60
    
    verify_gateway
    verify_httproutes
    display_access_info
    
    print_success "Deployment script completed successfully!"
}

# Run main function
main
