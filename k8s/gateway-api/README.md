# Kubernetes Gateway API Configuration

This directory contains Gateway API resources for the retail store application.

## Overview

The Gateway API is the successor to the Kubernetes Ingress API, providing a more expressive, extensible, and role-oriented API for configuring load balancing and traffic routing.

## Migration from NGINX Ingress Controller

This project has migrated from NGINX Ingress Controller to the AWS Gateway API Controller, which uses Amazon VPC Lattice as the underlying infrastructure.

### Key Changes

1. **Infrastructure Layer (Terraform)**
   - Removed: `enable_ingress_nginx` and associated configuration
   - Added: `enable_aws_gateway_api_controller` for AWS Gateway API Controller

2. **Application Layer (Helm Charts)**
   - Added: HTTPRoute resources to replace Ingress resources
   - Updated: values.yaml with Gateway API configuration options
   - Maintained: Legacy Ingress resources (disabled by default) for backward compatibility

### Benefits of Gateway API

- **Role-Oriented**: Separates infrastructure (Gateway) from application routing (HTTPRoute)
- **More Expressive**: Advanced routing capabilities including header-based routing, traffic splitting, and more
- **Vendor Agnostic**: Standard API across different implementations (AWS, GCP, Azure, NGINX, Istio, etc.)
- **Future-Proof**: Official Kubernetes API (v1 stable since Oct 2023)
- **Better Security**: Fine-grained RBAC and policy controls

## Resources

### Gateway (`gateway.yaml`)

Defines the entry point for traffic into the cluster:
- **HTTP Listener**: Port 80 for direct access
- **HTTPS Listener**: Port 443 for domain-based access with TLS termination

### HTTPRoute (in Helm charts)

Defines routing rules for the UI service:
- **Direct Route**: HTTP access without hostname restriction
- **Domain Route**: HTTPS access for retail-store.trainwithshubham.com

## Deployment

### Prerequisites

1. EKS cluster with Kubernetes 1.33+
2. AWS Gateway API Controller installed (automatically via Terraform)
3. cert-manager for SSL certificate management

### Apply Gateway Resource

```bash
# The Gateway resource is deployed via Terraform and kubectl
kubectl apply -f gateway.yaml
```

### Deploy Application with Gateway API

```bash
# Deploy UI service with Gateway API enabled
helm upgrade --install retail-store-ui ./src/ui/chart \
  --set gateway.enabled=true \
  --set ingress.enabled=false
```

## Verification

### Automated Validation

Run the validation script to check all Gateway API components:
```bash
./k8s/gateway-api/validate.sh
```

This script checks:
- Gateway API CRDs installation
- AWS Gateway API Controller status
- GatewayClass configuration
- Gateway status and address
- HTTPRoute resources
- cert-manager integration

### Manual Verification

Check Gateway status:
```bash
kubectl get gateway retail-store-gateway -n default
kubectl describe gateway retail-store-gateway -n default
```

Check HTTPRoute status:
```bash
kubectl get httproute -n default
kubectl describe httproute <route-name> -n default
```

## Troubleshooting

### Gateway not ready

If the Gateway is not ready, check the AWS Gateway API Controller logs:
```bash
kubectl logs -n aws-gateway-system -l control-plane=gateway-api-controller
```

### HTTPRoute not working

1. Verify the Gateway is ready and accepting routes
2. Check HTTPRoute status and events:
   ```bash
   kubectl describe httproute <route-name> -n default
   ```
3. Verify service and endpoints exist:
   ```bash
   kubectl get svc,endpoints -n default
   ```

## AWS VPC Lattice Integration

The AWS Gateway API Controller creates and manages AWS VPC Lattice resources:
- **Service Network**: Created from the Gateway
- **Services**: Created from HTTPRoutes
- **Target Groups**: Automatically configured for Kubernetes Services

To view VPC Lattice resources:
```bash
aws vpc-lattice list-service-networks
aws vpc-lattice list-services --service-network-identifier <id>
```

## References

- [Kubernetes Gateway API Documentation](https://gateway-api.sigs.k8s.io/)
- [AWS Gateway API Controller](https://www.gateway-api-controller.eks.aws.dev/)
- [Amazon VPC Lattice](https://docs.aws.amazon.com/vpc-lattice/)
