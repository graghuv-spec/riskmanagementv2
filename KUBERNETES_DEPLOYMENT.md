# Kubernetes Three-Tier Deployment

This project is packaged as a three-tier Kubernetes architecture:

- Presentation tier: frontend deployment + service
- Application tier: backend deployment + service
- Data tier: postgres deployment + service + persistent volume claim

The manifests are organized with Kustomize for cloud portability.

## Layout

- k8s/base: reusable baseline manifests
- k8s/overlays/local: local cluster settings (NodePort frontend)
- k8s/overlays/cloud: cloud settings (Ingress + scaled app tiers)
- helm/riskmanagement: Helm chart for one-command environment deployments

## Prerequisites

- kubectl
- Kubernetes cluster context configured
- Optional: ingress-nginx controller for cloud overlay ingress

## Images

Default image names in manifests:

- riskmanagementv2-backend:latest
- riskmanagementv2-frontend:latest
- postgres:15

For cloud deployment, push backend/frontend images to your registry and set them at deploy time.

## Deploy Local Overlay

```powershell
.\scripts\k8s-deploy.ps1 -Overlay local
```

Access frontend using NodePort:

- http://<node-ip>:30080

## Deploy Cloud Overlay

```powershell
.\scripts\k8s-deploy.ps1 -Overlay cloud -BackendImage <registry>/riskmanagement-backend:<tag> -FrontendImage <registry>/riskmanagement-frontend:<tag>
```

Cloud overlay adds:

- Ingress at host riskmanagement.example.com (edit in k8s/overlays/cloud/ingress.yaml)
- 2 replicas for backend and frontend

## Helm Deployment (Recommended)

Deploy local profile:

```powershell
.\scripts\helm-deploy.ps1 -Environment local
```

Deploy cloud profile:

```powershell
.\scripts\helm-deploy.ps1 -Environment cloud
```

Destroy Helm release:

```powershell
.\scripts\helm-destroy.ps1
```

Helm values files:

- helm/riskmanagement/values.yaml (base defaults)
- helm/riskmanagement/values-local.yaml (local overrides)
- helm/riskmanagement/values-cloud.yaml (cloud overrides)

## Verify

```powershell
kubectl -n riskmanagement get pods
kubectl -n riskmanagement get svc
kubectl -n riskmanagement get ingress
```

## Configuration

Main runtime configuration is provided by:

- backend-config ConfigMap
- frontend-config ConfigMap
- postgres-secret Secret

Update these files for environment-specific values:

- k8s/base/backend-configmap.yaml
- k8s/base/frontend-configmap.yaml
- k8s/base/postgres-secret.yaml

## Remove Deployment

```powershell
.\scripts\k8s-destroy.ps1
```

## Notes For Cloud Services

This layout is cloud-neutral and works on:

- AKS
- EKS
- GKE
- DigitalOcean Kubernetes
- OpenShift (with minor route/security adjustments)

To productionize further, next steps are:

1. Replace postgres deployment with managed database service
2. Move secrets to cloud secret manager integration
3. Add HPA and resource limits tuning
4. Add TLS certificates on ingress
5. Add CI/CD pipeline for image build and kubectl apply/helm release
