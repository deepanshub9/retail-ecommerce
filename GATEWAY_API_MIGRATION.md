# Gateway API Migration Guide

This guide explains the migration from NGINX Ingress Controller to Kubernetes Gateway API with AWS VPC Lattice.

## What Changed

### Architecture Changes

**Before (NGINX Ingress):**

```
Internet → AWS NLB → NGINX Ingress Controller → Services → Pods
```

**After (Gateway API):**

```
Internet → AWS VPC Lattice → Services → Pods
```

### Key Benefits

1. **Cloud-Native Integration**: VPC Lattice is deeply integrated with AWS
2. **Better Traffic Management**: Native support for weighted routing, canary deployments
3. **Role Separation**: Platform team manages Gateway, dev teams manage HTTPRoutes
4. **Standards-Based**: Kubernetes SIG-Network standard (GA in K8s 1.28+)
5. **Advanced Features**: Header-based routing, cross-namespace routing, policy attachments

## Migration Steps

### Step 1: Understand the New Resources

**GatewayClass** (Platform Admin)

- Defines the controller implementation (AWS VPC Lattice)
- Created once per cluster
- Managed by Terraform

**Gateway** (Platform/DevOps Team)

- Defines the load balancer configuration
- Listens on specific ports (80, 443)
- Created per namespace or shared
- Managed by Terraform

**HTTPRoute** (Development Team)

- Defines routing rules for your application
- References a Gateway
- Managed by Helm charts per service

### Step 2: Deploy New Infrastructure

```bash
cd terraform

# Initialize Terraform with updated providers
terraform init -upgrade

# Review the changes
terraform plan

# Apply Gateway API changes
terraform apply
```

### Step 3: Verify Gateway API Installation

```bash
# Check Gateway API CRDs are installed
kubectl get crd | grep gateway

# Verify GatewayClass
kubectl get gatewayclass

# Check Gateway status
kubectl get gateway -n retail-store

# View HTTPRoutes
kubectl get httproute -n retail-store
```

### Step 4: Update Application Configuration

The UI service is already configured with Gateway API support. Other services need similar updates.

**For each service:**

1. Add `gateway.yaml` template to chart/templates/
2. Update `values.yaml` with Gateway configuration
3. Set `gateway.enabled: true` and `ingress.enabled: false`

### Step 5: Get Application URL

```bash
# Get Gateway DNS endpoint
kubectl get gateway retail-store-gateway -n retail-store \
  -o jsonpath='{.status.addresses[0].value}'

# Or use terraform output
terraform output retail_store_url
```

### Step 6: Test the Application

```bash
# Get the Gateway endpoint
GATEWAY_URL=$(kubectl get gateway retail-store-gateway -n retail-store \
  -o jsonpath='{.status.addresses[0].value}')

# Test UI service
curl http://${GATEWAY_URL}/

# Check health
curl http://${GATEWAY_URL}/actuator/health
```

## Configuration Differences

### Ingress (Old) vs HTTPRoute (New)

**Old (Ingress):**

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ui
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "false"
spec:
  ingressClassName: nginx
  rules:
    - http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: ui
                port:
                  number: 80
```

**New (HTTPRoute):**

```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: ui
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
        - name: ui
          port: 80
```

## Advanced Traffic Management

### Canary Deployments

```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: ui-canary
spec:
  parentRefs:
    - name: retail-store-gateway
  rules:
    - matches:
        - path:
            type: PathPrefix
            value: /
      backendRefs:
        - name: ui-stable
          port: 80
          weight: 90
        - name: ui-canary
          port: 80
          weight: 10
```

### Header-Based Routing

```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: ui-beta
spec:
  parentRefs:
    - name: retail-store-gateway
  rules:
    - matches:
        - headers:
            - name: X-Beta-User
              value: "true"
          path:
            type: PathPrefix
            value: /
      backendRefs:
        - name: ui-beta
          port: 80
```

### Cross-Namespace Routing

```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: shared-service
  namespace: app-team-a
spec:
  parentRefs:
    - name: shared-gateway
      namespace: platform-team
  rules:
    - matches:
        - path:
            type: PathPrefix
            value: /
      backendRefs:
        - name: my-service
          port: 80
```

## Security Considerations

### VPC Lattice Security Groups

The Terraform configuration automatically creates security groups that:

- Allow traffic from VPC Lattice managed prefix list
- Restrict traffic to necessary ports (80, 443, 8080)
- Enable VPC-internal communication

### Network Policies

Consider adding Kubernetes Network Policies:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-vpc-lattice
  namespace: retail-store
spec:
  podSelector:
    matchLabels:
      app: ui
  policyTypes:
    - Ingress
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              name: aws-application-networking-system
      ports:
        - protocol: TCP
          port: 80
```

## Monitoring and Observability

### VPC Lattice Metrics

View VPC Lattice services:

```bash
aws vpc-lattice list-services --region us-east-1
```

Get service details:

```bash
aws vpc-lattice get-service --service-identifier <service-id>
```

### CloudWatch Metrics

VPC Lattice automatically publishes metrics to CloudWatch:

- Request count
- Active connection count
- Processed bytes
- HTTP status codes
- Target health

Access via CloudWatch console under `AWS/VPCLattice` namespace.

### Gateway Status

```bash
# Detailed Gateway status
kubectl describe gateway retail-store-gateway -n retail-store

# HTTPRoute status
kubectl describe httproute ui-route -n retail-store
```

## Troubleshooting

### Gateway Not Ready

```bash
# Check Gateway API Controller logs
kubectl logs -n aws-application-networking-system \
  -l app.kubernetes.io/name=gateway-api-controller

# Verify GatewayClass
kubectl describe gatewayclass amazon-vpc-lattice
```

### HTTPRoute Not Attached

```bash
# Check HTTPRoute status
kubectl get httproute ui-route -n retail-store -o yaml

# Common issues:
# 1. Gateway not in same namespace (add namespace to parentRefs)
# 2. Service name mismatch (verify service name)
# 3. Port mismatch (check service port)
```

### No DNS Endpoint

```bash
# Gateway may take 2-3 minutes to get an address
kubectl get gateway retail-store-gateway -n retail-store -w

# Check VPC Lattice service creation
aws vpc-lattice list-services --region us-east-1
```

### Traffic Not Reaching Pods

```bash
# Verify security groups
aws ec2 describe-security-groups \
  --filters "Name=tag:Name,Values=*vpc-lattice*"

# Check VPC Lattice prefix list
aws ec2 describe-managed-prefix-lists \
  --filters "Name=prefix-list-name,Values=com.amazonaws.*.vpc-lattice"

# Verify pod endpoints
kubectl get endpoints -n retail-store
```

## Cost Considerations

### VPC Lattice Pricing

- **Service Network**: $0.025 per hour
- **Service**: $0.025 per hour per service
- **Data Processing**: $0.025 per GB processed

Estimate: ~$18-36/month for base infrastructure + data processing costs.

### Cost Optimization

1. **Share Service Networks**: Use one service network across multiple services
2. **Right-Size Services**: Remove unused services
3. **Monitor Data Transfer**: Track GB processed in CloudWatch

## Rollback Plan

If you need to rollback to NGINX Ingress:

```bash
# 1. Re-enable NGINX in addons.tf
# Uncomment the nginx ingress section and comment Gateway API

# 2. Update UI values.yaml
gateway:
  enabled: false
ingress:
  enabled: true

# 3. Apply Terraform changes
terraform apply

# 4. Redeploy applications via ArgoCD
kubectl patch app retail-store-ui -n argocd \
  -p '{"spec":{"syncPolicy":{"automated":null}}}' --type merge
kubectl argocd app sync retail-store-ui
```

## Best Practices

1. **Use Gateway API for New Services**: Start with Gateway API for greenfield deployments
2. **Gradual Migration**: Migrate one service at a time
3. **Test Thoroughly**: Verify each service before moving to the next
4. **Monitor Metrics**: Use CloudWatch to track VPC Lattice performance
5. **Document Routes**: Maintain clear documentation of all HTTPRoutes
6. **Use Labels**: Tag Gateways and Routes with team/service labels
7. **Implement Network Policies**: Add Kubernetes Network Policies for defense in depth

## Additional Resources

- [Kubernetes Gateway API Documentation](https://gateway-api.sigs.k8s.io/)
- [AWS VPC Lattice Documentation](https://docs.aws.amazon.com/vpc-lattice/)
- [AWS Gateway API Controller](https://www.gateway-api-controller.eks.aws.dev/)
- [Gateway API Examples](https://github.com/kubernetes-sigs/gateway-api/tree/main/examples)

## Support

For issues or questions:

- GitHub Issues: [retail-store-sample-app/issues](https://github.com/LondheShubham153/retail-store-sample-app/issues)
- AWS Support: For VPC Lattice specific issues
- Kubernetes SIG-Network: For Gateway API questions
