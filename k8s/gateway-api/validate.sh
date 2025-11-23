#!/bin/bash
# Gateway API Validation Script
# This script validates the Gateway API installation and configuration

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "================================================"
echo "Gateway API Validation Script"
echo "================================================"
echo ""

# Function to print status
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
    else
        echo -e "${RED}✗${NC} $2"
    fi
}

# Function to print warning
print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Check if kubectl is installed
echo "Checking prerequisites..."
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}Error: kubectl is not installed${NC}"
    exit 1
fi
print_status 0 "kubectl is installed"

# Check if connected to a cluster
if ! kubectl cluster-info &> /dev/null; then
    echo -e "${RED}Error: Not connected to a Kubernetes cluster${NC}"
    exit 1
fi
print_status 0 "Connected to Kubernetes cluster"
echo ""

# Check Gateway API CRDs
echo "Checking Gateway API CRDs..."
CRDS=("gatewayclasses.gateway.networking.k8s.io" "gateways.gateway.networking.k8s.io" "httproutes.gateway.networking.k8s.io")
CRD_INSTALLED=true

for crd in "${CRDS[@]}"; do
    if kubectl get crd "$crd" &> /dev/null; then
        print_status 0 "$crd exists"
    else
        print_status 1 "$crd not found"
        CRD_INSTALLED=false
    fi
done
echo ""

if [ "$CRD_INSTALLED" = false ]; then
    print_warning "Gateway API CRDs are not installed. They will be installed by the AWS Gateway API Controller."
    echo ""
fi

# Check AWS Gateway API Controller
echo "Checking AWS Gateway API Controller..."
if kubectl get namespace aws-gateway-system &> /dev/null; then
    print_status 0 "aws-gateway-system namespace exists"
    
    # Check controller deployment
    if kubectl get deployment -n aws-gateway-system gateway-api-controller &> /dev/null; then
        print_status 0 "gateway-api-controller deployment exists"
        
        # Check if deployment is ready
        READY=$(kubectl get deployment -n aws-gateway-system gateway-api-controller -o jsonpath='{.status.readyReplicas}')
        DESIRED=$(kubectl get deployment -n aws-gateway-system gateway-api-controller -o jsonpath='{.status.replicas}')
        if [ "$READY" = "$DESIRED" ] && [ "$READY" != "" ]; then
            print_status 0 "gateway-api-controller is ready ($READY/$DESIRED)"
        else
            print_status 1 "gateway-api-controller is not ready ($READY/$DESIRED)"
        fi
    else
        print_status 1 "gateway-api-controller deployment not found"
    fi
else
    print_status 1 "aws-gateway-system namespace not found"
    print_warning "AWS Gateway API Controller is not installed. Install it via Terraform."
fi
echo ""

# Check GatewayClass
echo "Checking GatewayClass..."
if kubectl get gatewayclass amazon-vpc-lattice &> /dev/null; then
    print_status 0 "GatewayClass 'amazon-vpc-lattice' exists"
    
    # Check GatewayClass status
    ACCEPTED=$(kubectl get gatewayclass amazon-vpc-lattice -o jsonpath='{.status.conditions[?(@.type=="Accepted")].status}')
    if [ "$ACCEPTED" = "True" ]; then
        print_status 0 "GatewayClass is accepted"
    else
        print_status 1 "GatewayClass is not accepted"
    fi
else
    print_status 1 "GatewayClass 'amazon-vpc-lattice' not found"
    print_warning "Apply the Gateway resource: kubectl apply -f k8s/gateway-api/gateway.yaml"
fi
echo ""

# Check Gateway
echo "Checking Gateway..."
if kubectl get gateway retail-store-gateway -n default &> /dev/null; then
    print_status 0 "Gateway 'retail-store-gateway' exists"
    
    # Check Gateway status
    ACCEPTED=$(kubectl get gateway retail-store-gateway -n default -o jsonpath='{.status.conditions[?(@.type=="Accepted")].status}')
    PROGRAMMED=$(kubectl get gateway retail-store-gateway -n default -o jsonpath='{.status.conditions[?(@.type=="Programmed")].status}')
    
    if [ "$ACCEPTED" = "True" ]; then
        print_status 0 "Gateway is accepted"
    else
        print_status 1 "Gateway is not accepted"
    fi
    
    if [ "$PROGRAMMED" = "True" ]; then
        print_status 0 "Gateway is programmed"
    else
        print_status 1 "Gateway is not programmed"
    fi
    
    # Get Gateway address
    ADDRESS=$(kubectl get gateway retail-store-gateway -n default -o jsonpath='{.status.addresses[0].value}')
    if [ -n "$ADDRESS" ]; then
        print_status 0 "Gateway address: $ADDRESS"
    else
        print_warning "Gateway address not yet assigned"
    fi
else
    print_status 1 "Gateway 'retail-store-gateway' not found"
    print_warning "Apply the Gateway resource: kubectl apply -f k8s/gateway-api/gateway.yaml"
fi
echo ""

# Check HTTPRoutes
echo "Checking HTTPRoutes..."
HTTPROUTES=$(kubectl get httproute -n default -o name 2>/dev/null)
if [ -n "$HTTPROUTES" ]; then
    COUNT=$(echo "$HTTPROUTES" | wc -l)
    print_status 0 "Found $COUNT HTTPRoute(s)"
    
    for route in $HTTPROUTES; do
        NAME=$(echo "$route" | cut -d'/' -f2)
        ACCEPTED=$(kubectl get httproute "$NAME" -n default -o jsonpath='{.status.parents[0].conditions[?(@.type=="Accepted")].status}')
        
        if [ "$ACCEPTED" = "True" ]; then
            print_status 0 "HTTPRoute '$NAME' is accepted"
        else
            print_status 1 "HTTPRoute '$NAME' is not accepted"
        fi
    done
else
    print_warning "No HTTPRoutes found. Deploy the application with Gateway API enabled."
fi
echo ""

# Check cert-manager
echo "Checking cert-manager..."
if kubectl get namespace cert-manager &> /dev/null; then
    print_status 0 "cert-manager namespace exists"
    
    # Check cert-manager deployment
    if kubectl get deployment -n cert-manager cert-manager &> /dev/null; then
        print_status 0 "cert-manager deployment exists"
        
        READY=$(kubectl get deployment -n cert-manager cert-manager -o jsonpath='{.status.readyReplicas}')
        DESIRED=$(kubectl get deployment -n cert-manager cert-manager -o jsonpath='{.status.replicas}')
        if [ "$READY" = "$DESIRED" ] && [ "$READY" != "" ]; then
            print_status 0 "cert-manager is ready ($READY/$DESIRED)"
        else
            print_status 1 "cert-manager is not ready ($READY/$DESIRED)"
        fi
    fi
    
    # Check ClusterIssuer
    if kubectl get clusterissuer letsencrypt-prod &> /dev/null; then
        print_status 0 "ClusterIssuer 'letsencrypt-prod' exists"
    else
        print_warning "ClusterIssuer 'letsencrypt-prod' not found"
    fi
else
    print_status 1 "cert-manager namespace not found"
fi
echo ""

echo "================================================"
echo "Validation Complete"
echo "================================================"
echo ""
echo "Next steps:"
echo "1. If Gateway API Controller is not installed, run: terraform apply"
echo "2. If Gateway is not deployed, run: kubectl apply -f k8s/gateway-api/gateway.yaml"
echo "3. If HTTPRoutes are not deployed, deploy the application with gateway.enabled=true"
echo ""
