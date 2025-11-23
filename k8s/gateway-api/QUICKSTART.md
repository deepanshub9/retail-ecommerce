# Gateway API Quick Start Guide

This guide provides quick commands to get started with Gateway API in the retail store application.

## Prerequisites

- EKS cluster running (Kubernetes 1.33+)
- kubectl configured
- Terraform applied (installs AWS Gateway API Controller)

## Step-by-Step Deployment

### 1. Verify Gateway API Controller Installation

```bash
# Check if AWS Gateway API Controller is running
kubectl get pods -n aws-gateway-system

# Expected output:
# NAME                                     READY   STATUS    RESTARTS   AGE
# gateway-api-controller-xxxxxxxxxx-xxxxx   1/1     Running   0          5m
```

### 2. Deploy Gateway Resource

```bash
# Apply the Gateway resource
kubectl apply -f k8s/gateway-api/gateway.yaml

# Verify Gateway is created
kubectl get gateway retail-store-gateway -n default

# Check Gateway status (should show Accepted: True, Programmed: True)
kubectl describe gateway retail-store-gateway -n default
```

### 3. Deploy Application with Gateway API

```bash
# Deploy UI service with Gateway API enabled
helm upgrade --install retail-store-ui ./src/ui/chart \
  --set gateway.enabled=true \
  --set ingress.enabled=false \
  --namespace default

# Or use specific gateway configurations
helm upgrade --install retail-store-ui ./src/ui/chart \
  --set gateway.enabled=false \
  --set-string 'gateways[0].name=direct' \
  --set-string 'gateways[0].gatewayName=retail-store-gateway' \
  --set-string 'gateways[0].namespace=default' \
  --set-string 'gateways[0].sectionName=http' \
  --namespace default
```

### 4. Verify HTTPRoute Resources

```bash
# List all HTTPRoutes
kubectl get httproute -n default

# Check specific HTTPRoute details
kubectl describe httproute retail-store-ui-direct -n default
kubectl describe httproute retail-store-ui-domain -n default
```

### 5. Get Gateway Endpoint

```bash
# Get the Gateway address
kubectl get gateway retail-store-gateway -n default -o jsonpath='{.status.addresses[0].value}'

# Or use the full command to see all details
kubectl get gateway retail-store-gateway -n default -o yaml
```

### 6. Test Access

```bash
# Get the Gateway endpoint
GATEWAY_ENDPOINT=$(kubectl get gateway retail-store-gateway -n default -o jsonpath='{.status.addresses[0].value}')

# Test HTTP access
curl http://$GATEWAY_ENDPOINT/

# Test with domain (if configured)
curl -H "Host: retail-store.trainwithshubham.com" http://$GATEWAY_ENDPOINT/
```

## Quick Validation

Run the automated validation script:

```bash
./k8s/gateway-api/validate.sh
```

This checks all Gateway API components and provides a comprehensive status report.

## Common Tasks

### View Gateway Logs

```bash
# View AWS Gateway API Controller logs
kubectl logs -n aws-gateway-system -l control-plane=gateway-api-controller --tail=100 -f
```

### Check cert-manager Status

```bash
# Verify cert-manager is running
kubectl get pods -n cert-manager

# Check ClusterIssuer
kubectl get clusterissuer

# View certificates
kubectl get certificate -n default
```

### Update Gateway Configuration

```bash
# Edit the Gateway resource
kubectl edit gateway retail-store-gateway -n default

# Or apply changes from file
kubectl apply -f k8s/gateway-api/gateway.yaml
```

### Update HTTPRoute

```bash
# Edit HTTPRoute
kubectl edit httproute retail-store-ui-direct -n default

# Or redeploy the Helm chart with new values
helm upgrade retail-store-ui ./src/ui/chart \
  --reuse-values \
  --set gateway.hostnames[0]=new-domain.example.com
```

## Troubleshooting Quick Fixes

### Gateway Not Ready

```bash
# Check controller status
kubectl get pods -n aws-gateway-system
kubectl logs -n aws-gateway-system -l control-plane=gateway-api-controller --tail=50

# Check GatewayClass
kubectl get gatewayclass
kubectl describe gatewayclass amazon-vpc-lattice
```

### HTTPRoute Not Working

```bash
# Check HTTPRoute status
kubectl get httproute -n default
kubectl describe httproute <route-name> -n default

# Verify parent Gateway reference
kubectl get gateway retail-store-gateway -n default

# Check service and endpoints
kubectl get svc,endpoints -n default
```

### SSL Certificate Issues

```bash
# Check certificate status
kubectl get certificate -n default
kubectl describe certificate tls-secret -n default

# Check cert-manager logs
kubectl logs -n cert-manager -l app=cert-manager --tail=50

# Verify ClusterIssuer
kubectl describe clusterissuer letsencrypt-prod
```

## AWS VPC Lattice Commands

The AWS Gateway API Controller uses VPC Lattice under the hood. You can view these resources:

```bash
# List service networks
aws vpc-lattice list-service-networks

# List services in a service network
aws vpc-lattice list-services --service-network-identifier <service-network-id>

# Get service details
aws vpc-lattice get-service --service-identifier <service-id>

# List target groups
aws vpc-lattice list-target-groups
```

## Migration from NGINX Ingress

If you're migrating from NGINX Ingress Controller:

```bash
# 1. Deploy Gateway API
kubectl apply -f k8s/gateway-api/gateway.yaml

# 2. Update application to use Gateway API
helm upgrade retail-store-ui ./src/ui/chart \
  --set gateway.enabled=true \
  --set ingress.enabled=false

# 3. Verify traffic is flowing through Gateway
kubectl get httproute -n default

# 4. (Optional) Remove NGINX Ingress Controller
# This is done via Terraform - see GATEWAY_API_MIGRATION.md
```

## References

- [Complete Migration Guide](../../GATEWAY_API_MIGRATION.md)
- [Gateway API Documentation](README.md)
- [Kubernetes Gateway API](https://gateway-api.sigs.k8s.io/)
- [AWS Gateway API Controller](https://www.gateway-api-controller.eks.aws.dev/)

## Support

For detailed migration instructions, troubleshooting, and advanced features, see:
- [Gateway API Migration Guide](../../GATEWAY_API_MIGRATION.md)
- [Main README](../../README.md)
