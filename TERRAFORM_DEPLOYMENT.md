# Terraform Deployment Guide - Gateway API

## What Was Fixed

The Gateway API CRDs error has been resolved by:

1. **Created `gateway-api-crds.tf`** - Installs Gateway API CRDs before any Gateway resources
2. **Updated `addons.tf`** - Added proper dependency chain
3. **Updated `.gitignore`** - Excludes temporary kubeconfig files

## Deployment Order

```
1. module.retail_app_eks (EKS Cluster)
   ↓
2. null_resource.install_gateway_api_crds (Install CRDs)
   ↓
3. time_sleep.wait_for_gateway_crds (Wait 45s)
   ↓
4. null_resource.verify_gateway_api_crds (Verify CRDs)
   ↓
5. module.eks_addons (Gateway API Controller + Cert Manager)
   ↓
6. time_sleep.wait_for_gateway_controller (Wait 90s)
   ↓
7. kubectl_manifest.gateway_class (Create GatewayClass)
   ↓
8. kubectl_manifest.retail_store_gateway (Create Gateway)
   ↓
9. kubectl_manifest.ui_http_route (Create HTTPRoute)
```

## How to Deploy

### Fresh Deployment (No existing resources)

```bash
cd terraform

# Initialize
terraform init

# Plan
terraform plan

# Apply
terraform apply
```

### If You Already Have VPC + EKS

Since you mentioned VPC and EKS are already created:

```bash
cd terraform

# Import existing resources (if needed)
terraform import module.vpc.aws_vpc.this <vpc-id>
terraform import module.retail_app_eks.aws_eks_cluster.this <cluster-name>

# Or just target the new resources
terraform apply -target=null_resource.install_gateway_api_crds
terraform apply -target=module.eks_addons
terraform apply -target=kubectl_manifest.gateway_class
terraform apply
```

### Step-by-Step Application (Recommended for Existing Infrastructure)

```bash
cd terraform

# Step 1: Install Gateway API CRDs
terraform apply -target=null_resource.install_gateway_api_crds -auto-approve

# Wait a moment
sleep 30

# Step 2: Verify CRDs
terraform apply -target=null_resource.verify_gateway_api_crds -auto-approve

# Step 3: Install addons (Gateway API Controller)
terraform apply -target=module.eks_addons -auto-approve

# Wait for controller to be ready
sleep 60

# Step 4: Create GatewayClass
terraform apply -target=kubectl_manifest.gateway_class -auto-approve

# Step 5: Apply everything else
terraform apply -auto-approve
```

## Verify Deployment

### Check Gateway API CRDs

```bash
# Update kubeconfig
aws eks update-kubeconfig --region us-east-1 --name <cluster-name>

# Verify CRDs are installed
kubectl get crd | grep gateway

# Expected output:
# gatewayclasses.gateway.networking.k8s.io
# gateways.gateway.networking.k8s.io
# httproutes.gateway.networking.k8s.io
# referencegrants.gateway.networking.k8s.io
```

### Check Gateway API Controller

```bash
# Check controller pods
kubectl get pods -n aws-application-networking-system

# Check controller logs
kubectl logs -n aws-application-networking-system \
  -l app.kubernetes.io/name=gateway-api-controller
```

### Check Gateway Resources

```bash
# Check GatewayClass
kubectl get gatewayclass

# Check Gateway
kubectl get gateway -n retail-store

# Check HTTPRoutes
kubectl get httproute -n retail-store

# Get Gateway DNS endpoint
kubectl get gateway retail-store-gateway -n retail-store \
  -o jsonpath='{.status.addresses[0].value}'
```

## Troubleshooting

### Error: CRDs still not found

If you still get CRD errors:

```bash
# Manually install CRDs
kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.0.0/standard-install.yaml

# Wait and verify
sleep 30
kubectl get crd | grep gateway

# Then run terraform apply again
cd terraform
terraform apply
```

### Error: Gateway API Controller not starting

```bash
# Check controller status
kubectl get pods -n aws-application-networking-system

# Check logs
kubectl logs -n aws-application-networking-system \
  -l app.kubernetes.io/name=gateway-api-controller --tail=100

# Check IAM permissions
aws eks describe-addon --cluster-name <cluster-name> \
  --addon-name aws-gateway-controller
```

### Error: Gateway not getting DNS endpoint

```bash
# This is normal - Gateway takes 2-3 minutes to provision
kubectl get gateway retail-store-gateway -n retail-store -w

# Check VPC Lattice services
aws vpc-lattice list-services --region us-east-1

# Check Gateway status
kubectl describe gateway retail-store-gateway -n retail-store
```

## Clean Up (If Needed)

```bash
# Destroy all resources
cd terraform
terraform destroy

# Or selectively destroy Gateway resources
terraform destroy -target=kubectl_manifest.ui_http_route
terraform destroy -target=kubectl_manifest.retail_store_gateway
terraform destroy -target=kubectl_manifest.gateway_class
terraform destroy -target=module.eks_addons
```

## Common Issues

### Issue 1: "resource isn't valid for cluster"

**Solution**: CRDs not installed. Run the manual CRD installation above.

### Issue 2: "timed out waiting for the condition"

**Solution**: Increase wait times in `time_sleep` resources or wait manually between steps.

### Issue 3: "context deadline exceeded"

**Solution**: Gateway API Controller may still be starting. Wait 2-3 minutes and retry.

### Issue 4: "no matches for kind GatewayClass"

**Solution**: CRDs installed but not ready. Wait 30 seconds and retry.

## Next Steps After Successful Deployment

1. **Get Gateway URL**

   ```bash
   terraform output retail_store_url
   ```

2. **Access Application**

   ```bash
   GATEWAY_URL=$(kubectl get gateway retail-store-gateway -n retail-store \
     -o jsonpath='{.status.addresses[0].value}')
   echo "Application: http://${GATEWAY_URL}"
   curl http://${GATEWAY_URL}
   ```

3. **Check ArgoCD**

   ```bash
   kubectl get applications -n argocd
   kubectl port-forward svc/argocd-server -n argocd 8080:443
   ```

4. **Monitor VPC Lattice**
   - AWS Console → VPC → Lattice → Services
   - CloudWatch → Metrics → AWS/VPCLattice

## Files Changed

- ✅ `terraform/gateway-api-crds.tf` (NEW) - CRD installation
- ✅ `terraform/addons.tf` (UPDATED) - Fixed dependency chain
- ✅ `.gitignore` (UPDATED) - Exclude .kubeconfig

## Key Points

- Gateway API CRDs must be installed **before** creating Gateway resources
- AWS Gateway API Controller is installed via EKS Blueprints addon
- Wait times are important - don't rush the apply process
- VPC Lattice service provisioning takes 2-3 minutes
- All temporary kubeconfig files are gitignored

Your Terraform code is now properly configured to handle Gateway API deployment! 🚀
