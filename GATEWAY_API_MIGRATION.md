# Gateway API Migration Guide

## Overview

This document provides guidance for migrating from NGINX Ingress Controller to Kubernetes Gateway API in the retail store application.

## What Changed?

### 1. Infrastructure Layer (Terraform)

**Before (NGINX Ingress Controller):**
```hcl
enable_ingress_nginx = true
ingress_nginx = {
  most_recent = true
  namespace   = "ingress-nginx"
  # ... NGINX-specific configuration
}
```

**After (Gateway API Controller):**
```hcl
enable_aws_gateway_api_controller = true
aws_gateway_api_controller = {
  most_recent = true
  namespace   = "aws-gateway-system"
}
```

### 2. Application Layer (Kubernetes Resources)

**Before (Ingress):**
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: retail-store-ui
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "false"
spec:
  ingressClassName: nginx
  rules:
    - host: retail-store.trainwithshubham.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: retail-store-ui
                port:
                  number: 80
```

**After (Gateway + HTTPRoute):**
```yaml
# Gateway (Infrastructure) - Deployed once
apiVersion: gateway.networking.k8s.io/v1
kind: Gateway
metadata:
  name: retail-store-gateway
spec:
  gatewayClassName: amazon-vpc-lattice
  listeners:
    - name: http
      protocol: HTTP
      port: 80
    - name: https
      protocol: HTTPS
      port: 443
      tls:
        certificateRefs:
          - name: tls-secret

---
# HTTPRoute (Application) - Per service
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: retail-store-ui-domain
spec:
  parentRefs:
    - name: retail-store-gateway
      sectionName: https
  hostnames:
    - retail-store.trainwithshubham.com
  rules:
    - backendRefs:
        - name: retail-store-ui
          port: 80
```

### 3. Helm Values Configuration

**Before (values.yaml):**
```yaml
ingress:
  enabled: true
  className: "nginx"
  hosts:
    - retail-store.trainwithshubham.com
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
```

**After (values.yaml):**
```yaml
gateway:
  enabled: true
  gatewayName: "retail-store-gateway"
  namespace: "default"
  sectionName: "https"
  hostnames:
    - retail-store.trainwithshubham.com

# Legacy ingress disabled
ingress:
  enabled: false
```

## Migration Steps

### Step 1: Update Terraform Configuration

1. Navigate to the Terraform directory:
   ```bash
   cd terraform/
   ```

2. The `addons.tf` file has been updated to use Gateway API Controller instead of NGINX Ingress.

3. Plan and apply the changes:
   ```bash
   terraform init -upgrade
   terraform plan
   terraform apply
   ```

   This will:
   - Install AWS Gateway API Controller
   - Remove NGINX Ingress Controller
   - Keep cert-manager running for SSL certificate management

### Step 2: Deploy Gateway Resource

1. Apply the Gateway resource:
   ```bash
   kubectl apply -f k8s/gateway-api/gateway.yaml
   ```

2. Verify Gateway is ready:
   ```bash
   kubectl get gateway retail-store-gateway -n default
   kubectl describe gateway retail-store-gateway -n default
   ```

   Wait until the Gateway status shows `Accepted: True` and `Programmed: True`.

### Step 3: Update Application Helm Charts

The UI service Helm chart has been updated to support Gateway API:

1. Update the UI service deployment:
   ```bash
   cd src/ui/chart
   helm upgrade --install retail-store-ui . \
     --set gateway.enabled=true \
     --set ingress.enabled=false
   ```

2. Verify HTTPRoute resources:
   ```bash
   kubectl get httproute -n default
   kubectl describe httproute retail-store-ui-direct -n default
   kubectl describe httproute retail-store-ui-domain -n default
   ```

### Step 4: Test and Validate

1. Get the Gateway endpoint:
   ```bash
   kubectl get gateway retail-store-gateway -n default -o jsonpath='{.status.addresses[0].value}'
   ```

2. Test HTTP access (direct):
   ```bash
   curl http://<gateway-endpoint>/
   ```

3. Test HTTPS access (domain):
   ```bash
   curl https://retail-store.trainwithshubham.com/
   ```

4. Verify SSL certificate:
   ```bash
   openssl s_client -connect retail-store.trainwithshubham.com:443 -servername retail-store.trainwithshubham.com
   ```

### Step 5: Clean Up (Optional)

If you want to completely remove the old NGINX Ingress resources:

```bash
# Remove NGINX Ingress namespace and resources
kubectl delete namespace ingress-nginx
```

## Rollback Plan

If you need to rollback to NGINX Ingress Controller:

1. **Revert Terraform changes:**
   ```bash
   cd terraform/
   # Revert addons.tf to use enable_ingress_nginx
   terraform apply
   ```

2. **Update Helm values:**
   ```bash
   helm upgrade --install retail-store-ui ./src/ui/chart \
     --set gateway.enabled=false \
     --set ingress.enabled=true
   ```

3. **Remove Gateway resources:**
   ```bash
   kubectl delete -f k8s/gateway-api/gateway.yaml
   ```

## Key Benefits of Gateway API

### 1. **Role-Oriented Design**
- **Infrastructure Admin**: Manages Gateway (load balancer)
- **Application Developer**: Manages HTTPRoute (routing rules)
- Clear separation of concerns

### 2. **More Expressive Routing**
- Header-based routing
- Query parameter matching
- Traffic splitting (A/B testing, canary deployments)
- Request/response header manipulation
- Redirect and rewrite capabilities

### 3. **Vendor Agnostic**
- Standard API across cloud providers
- Easy to switch between implementations
- Consistent behavior

### 4. **Better Security**
- Fine-grained RBAC per resource type
- Policy attachment points
- TLS configuration at Gateway level

### 5. **Future-Proof**
- Official Kubernetes API (v1 stable)
- Long-term replacement for Ingress
- Active development and community support

## Advanced Features (Coming Soon)

The Gateway API supports advanced features that we can leverage in the future:

### Traffic Splitting
```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
spec:
  rules:
    - backendRefs:
        - name: retail-store-ui-v1
          port: 80
          weight: 90
        - name: retail-store-ui-v2
          port: 80
          weight: 10
```

### Header-Based Routing
```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
spec:
  rules:
    - matches:
        - headers:
            - name: X-Version
              value: v2
      backendRefs:
        - name: retail-store-ui-v2
          port: 80
```

### Request/Response Modification
```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
spec:
  rules:
    - filters:
        - type: RequestHeaderModifier
          requestHeaderModifier:
            add:
              - name: X-Custom-Header
                value: custom-value
```

## Troubleshooting

### Gateway Not Ready

**Symptom:** Gateway status shows `Accepted: False`

**Solution:**
```bash
# Check controller logs
kubectl logs -n aws-gateway-system -l control-plane=gateway-api-controller

# Verify GatewayClass exists
kubectl get gatewayclass
```

### HTTPRoute Not Working

**Symptom:** HTTPRoute shows `Accepted: False` or traffic not routing

**Solution:**
```bash
# Check HTTPRoute status
kubectl describe httproute <name> -n default

# Verify parent Gateway exists and is ready
kubectl get gateway -n default

# Check service and endpoints
kubectl get svc,endpoints -n default
```

### SSL Certificate Issues

**Symptom:** HTTPS not working or certificate errors

**Solution:**
```bash
# Check cert-manager certificate
kubectl get certificate tls-secret -n default
kubectl describe certificate tls-secret -n default

# Check cert-manager logs
kubectl logs -n cert-manager -l app=cert-manager

# Verify ClusterIssuer
kubectl get clusterissuer letsencrypt-prod
```

### AWS VPC Lattice Issues

**Symptom:** Gateway API Controller not creating VPC Lattice resources

**Solution:**
```bash
# Check AWS permissions
aws sts get-caller-identity

# Verify VPC Lattice service networks
aws vpc-lattice list-service-networks

# Check controller logs
kubectl logs -n aws-gateway-system -l control-plane=gateway-api-controller
```

## References

- [Kubernetes Gateway API](https://gateway-api.sigs.k8s.io/)
- [AWS Gateway API Controller](https://www.gateway-api-controller.eks.aws.dev/)
- [Amazon VPC Lattice](https://docs.aws.amazon.com/vpc-lattice/)
- [Gateway API vs Ingress](https://gateway-api.sigs.k8s.io/concepts/api-overview/#from-ingress)

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review AWS Gateway API Controller documentation
3. Open an issue in the repository
