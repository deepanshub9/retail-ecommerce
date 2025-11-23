# Gateway API Quick Reference

## Essential Commands

### Check Status
```bash
# Gateway API Controller
kubectl get pods -n aws-application-networking-system

# GatewayClass
kubectl get gatewayclass

# Gateway
kubectl get gateway -n retail-store

# HTTPRoutes
kubectl get httproute -n retail-store

# All Gateway API resources
kubectl get gateway,httproute,gatewayclass -A
```

### Get Application URL
```bash
# Get Gateway DNS endpoint
kubectl get gateway retail-store-gateway -n retail-store \
  -o jsonpath='{.status.addresses[0].value}'

# Or with watch (wait for DNS)
kubectl get gateway retail-store-gateway -n retail-store -w

# Via Terraform
cd terraform && terraform output retail_store_url
```

### Debug Issues
```bash
# Gateway API Controller logs
kubectl logs -n aws-application-networking-system \
  -l app.kubernetes.io/name=gateway-api-controller --tail=100

# Gateway details
kubectl describe gateway retail-store-gateway -n retail-store

# HTTPRoute details
kubectl describe httproute ui-route -n retail-store

# Service endpoints
kubectl get endpoints -n retail-store
```

### VPC Lattice
```bash
# List services
aws vpc-lattice list-services --region <region>

# List service networks
aws vpc-lattice list-service-networks --region <region>

# Get service details
SERVICE_ID=$(aws vpc-lattice list-services --region <region> \
  --query 'items[0].id' --output text)
aws vpc-lattice get-service --service-identifier $SERVICE_ID
```

## Resource Hierarchy

```
GatewayClass (amazon-vpc-lattice)
    ↓
Gateway (retail-store-gateway)
    ↓
HTTPRoute (ui-route, cart-route, etc.)
    ↓
Service (retail-store-ui, etc.)
    ↓
Pods
```

## Configuration Patterns

### Basic HTTPRoute
```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: my-route
  namespace: retail-store
spec:
  parentRefs:
  - name: retail-store-gateway
  rules:
  - matches:
    - path:
        type: PathPrefix
        value: /
    backendRefs:
    - name: my-service
      port: 80
```

### With Hostname
```yaml
spec:
  parentRefs:
  - name: retail-store-gateway
  hostnames:
  - myapp.example.com
  rules:
  - backendRefs:
    - name: my-service
      port: 80
```

### Canary (90/10)
```yaml
spec:
  rules:
  - backendRefs:
    - name: my-service-stable
      port: 80
      weight: 90
    - name: my-service-canary
      port: 80
      weight: 10
```

### Header-Based
```yaml
spec:
  rules:
  - matches:
    - headers:
      - name: X-Beta-User
        value: "true"
    backendRefs:
    - name: my-service-beta
      port: 80
```

## Troubleshooting

### Gateway Not Ready
**Symptom**: Gateway stuck in "Not Ready" state

**Check**:
```bash
kubectl describe gateway retail-store-gateway -n retail-store
kubectl get events -n retail-store
kubectl logs -n aws-application-networking-system \
  -l app.kubernetes.io/name=gateway-api-controller
```

**Common Causes**:
- VPC Lattice not available in region
- IAM permissions missing
- Security group issues

### No DNS Endpoint
**Symptom**: Gateway has no address in status

**Wait**: Gateway provisioning takes 2-3 minutes

**Check**:
```bash
aws vpc-lattice list-services --region <region>
```

### HTTPRoute Not Attached
**Symptom**: HTTPRoute parent status shows "NotAdmitted"

**Check**:
```bash
kubectl describe httproute ui-route -n retail-store
```

**Common Causes**:
- Gateway doesn't exist
- Wrong namespace reference
- Service name mismatch
- Port mismatch

### Traffic Not Reaching Pods
**Symptom**: 503 errors or no response

**Check**:
```bash
# Verify service exists
kubectl get svc -n retail-store

# Check endpoints
kubectl get endpoints -n retail-store

# Verify pods are running
kubectl get pods -n retail-store

# Check security groups
aws ec2 describe-security-groups \
  --filters "Name=tag:Name,Values=*vpc-lattice*"
```

## Security Groups

VPC Lattice requires traffic from its managed prefix list:

```bash
# Get prefix list ID
PREFIX_LIST_ID=$(aws ec2 describe-managed-prefix-lists \
  --filters "Name=prefix-list-name,Values=com.amazonaws.*.vpc-lattice" \
  --query 'PrefixLists[0].PrefixListId' --output text)

# View prefix list entries
aws ec2 get-managed-prefix-list-entries \
  --prefix-list-id $PREFIX_LIST_ID
```

Security group rule should allow:
- **Source**: VPC Lattice prefix list
- **Ports**: All TCP (or specific app ports)
- **Protocol**: TCP

## Monitoring

### CloudWatch Metrics
Namespace: `AWS/VPCLattice`

Key metrics:
- `RequestCount` - Total requests
- `ActiveConnectionCount` - Current connections
- `ProcessedBytes` - Data transfer
- `HTTPCode_Target_2XX_Count` - Success responses
- `HTTPCode_Target_4XX_Count` - Client errors
- `HTTPCode_Target_5XX_Count` - Server errors
- `TargetResponseTime` - Latency

### View in Console
AWS Console → VPC → Lattice → Services

## Cost Optimization

- **Service Network**: $0.025/hour (~$18/month)
- **Service**: $0.025/hour per service
- **Data Processing**: $0.025/GB

**Tips**:
- Share one service network across services
- Use single Gateway for multiple routes
- Monitor data transfer in CloudWatch

## Migration Checklist

- [ ] Update Terraform (`addons.tf`, `security.tf`, `outputs.tf`)
- [ ] Run `terraform init -upgrade`
- [ ] Run `terraform apply`
- [ ] Verify GatewayClass: `kubectl get gatewayclass`
- [ ] Verify Gateway: `kubectl get gateway -n retail-store`
- [ ] Add Gateway templates to each service chart
- [ ] Update `values.yaml` (`gateway.enabled: true`)
- [ ] Deploy/sync applications via ArgoCD
- [ ] Verify HTTPRoutes: `kubectl get httproute -n retail-store`
- [ ] Get Gateway URL and test application
- [ ] Update DNS (if using custom domain)
- [ ] Monitor CloudWatch metrics
- [ ] Disable old Ingress resources

## Quick Deploy

```bash
# Clone repo
git clone <your-repo>
cd retail-store-sample-app

# Deploy everything
cd terraform
terraform init
terraform apply -auto-approve

# Configure kubectl
aws eks update-kubeconfig \
  --name $(terraform output -raw cluster_name) \
  --region <your-region>

# Wait for Gateway (2-3 minutes)
kubectl get gateway retail-store-gateway -n retail-store -w

# Get URL
GATEWAY_URL=$(kubectl get gateway retail-store-gateway -n retail-store \
  -o jsonpath='{.status.addresses[0].value}')
echo "Application URL: http://${GATEWAY_URL}"
```

## Additional Resources

- [Full Migration Guide](./GATEWAY_API_MIGRATION.md)
- [Implementation Summary](./IMPLEMENTATION_SUMMARY.md)
- [Kubernetes Gateway API Docs](https://gateway-api.sigs.k8s.io/)
- [AWS VPC Lattice Docs](https://docs.aws.amazon.com/vpc-lattice/)
- [AWS Gateway Controller](https://www.gateway-api-controller.eks.aws.dev/)
