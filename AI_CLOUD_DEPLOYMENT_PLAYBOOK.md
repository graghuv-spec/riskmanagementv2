# AI Cloud Deployment Playbook

This document is written so another engineer can hand it to an AI assistant and ask it to deploy this repository to their own cloud account.

## 1. What This Deploys

- Frontend: Angular app (containerized)
- Backend: Spring Boot app (containerized)
- Database: Managed PostgreSQL (external to cluster)
- Runtime: Kubernetes + Helm
- TLS: cert-manager + Let's Encrypt (for public domains)

## 2. Required Prerequisites

## 2.1 Local machine tools

- Git
- Docker
- Kubernetes CLI: kubectl
- Helm v3+
- PowerShell 5.1+ (or PowerShell 7+)

## 2.2 Cloud prerequisites (any cloud)

- A Kubernetes cluster reachable from kubectl
- A container registry for backend/frontend images
- A managed PostgreSQL instance
- Public ingress/load balancer for HTTP/HTTPS
- DNS control for your domain (for trusted TLS)

## 2.3 Google Cloud specific prerequisites

- Google Cloud SDK installed
- gcloud authenticated
- gke-gcloud-auth-plugin installed
- APIs enabled:
  - container.googleapis.com
  - artifactregistry.googleapis.com
  - cloudbuild.googleapis.com
  - sqladmin.googleapis.com

## 2.4 Azure specific prerequisites

- Azure CLI installed and authenticated
- AKS cluster created (or permissions to create it)
- ACR created for container images
- Azure Database for PostgreSQL Flexible Server created
- NGINX ingress controller installed in the cluster
- cert-manager installed in the cluster

## 2.5 AWS specific prerequisites

- AWS CLI installed and authenticated
- EKS cluster created (or permissions to create it)
- ECR repositories created for backend/frontend images
- Amazon RDS for PostgreSQL created
- NGINX ingress controller installed in the cluster
- cert-manager installed in the cluster

## 3. Deployment Inputs To Collect First

Prepare the values below before asking an AI to deploy.

```yaml
cloud:
  provider: gcp|aws|azure|other
  projectOrSubscription: "<id>"
  region: "<region>"
  clusterName: "<k8s-cluster-name>"

registry:
  backendRepository: "<registry>/<path>/riskmanagement-backend"
  backendTag: "<tag>"
  frontendRepository: "<registry>/<path>/riskmanagement-frontend"
  frontendTag: "<tag>"

database:
  host: "<managed-postgres-host-or-ip>"
  port: 5432
  name: "postgres"
  user: "postgres"
  password: "<db-password>"

ingress:
  domain: "app.example.com"
  ingressClass: "nginx"
  enableTls: true
  clusterIssuer: "letsencrypt-prod"

k8s:
  namespace: "riskmanagement"
  releaseName: "riskmanagement-cloud"
```

## 4. AI Execution Contract (Copy/Paste To Any AI)

Use this exact prompt with your values filled in.

```text
You are deploying RiskManagement Pro from this repository using existing scripts.

Requirements:
1) Use managed PostgreSQL (DO NOT deploy in-cluster postgres).
2) Deploy frontend + backend to Kubernetes with Helm.
3) Configure ingress host and TLS certificate.
4) Validate login, dashboard load, and /api/loans.
5) If deployment fails, fix and continue until healthy.

Use these inputs:
- Provider: <provider>
- Project/Subscription: <id>
- Region: <region>
- Cluster: <cluster>
- Namespace: riskmanagement
- Helm release: riskmanagement-cloud
- Backend image: <backendRepository>:<backendTag>
- Frontend image: <frontendRepository>:<frontendTag>
- DB host: <dbHost>
- DB port: 5432
- DB name: <dbName>
- DB user: <dbUser>
- DB password: <dbPassword>
- Domain: <domain>
- Ingress class: nginx
- TLS enabled: true
- Cluster issuer: letsencrypt-prod

Repository scripts to use:
- scripts/helm-deploy-cloud-managed.ps1
- scripts/gcp-gke-deploy.ps1 (GCP only)

After deployment, print:
- ingress URL
- certificate status
- backend/frontend pod readiness
- login credentials currently valid
```

## 4.1 Azure-only AI prompt template

Use this when the target platform is AKS.

```text
Deploy RiskManagement Pro to Azure AKS using managed PostgreSQL and existing repository scripts.

Requirements:
1) Use AKS as Kubernetes runtime.
2) Use Azure Database for PostgreSQL Flexible Server (do not deploy in-cluster postgres).
3) Use ACR images provided as input.
4) Configure ingress + TLS for the provided domain.
5) Validate application health and login; keep fixing until successful.

Inputs:
- Subscription ID: <subscriptionId>
- AKS resource group: <aksResourceGroup>
- AKS cluster: <aksClusterName>
- Region: <region>
- Backend image: <acrLoginServer>/<repo>/riskmanagement-backend:<backendTag>
- Frontend image: <acrLoginServer>/<repo>/riskmanagement-frontend:<frontendTag>
- DB host: <postgresFlexibleServerHost>
- DB port: 5432
- DB name: <dbName>
- DB user: <dbUser>
- DB password: <dbPassword>
- Domain: <domain>

Execution path:
1) az login
2) az account set --subscription <subscriptionId>
3) az aks get-credentials --resource-group <aksResourceGroup> --name <aksClusterName> --overwrite-existing
4) Run scripts/helm-deploy-cloud-managed.ps1 with the provided values and -EnableTls

After deployment, print:
- current kubectl context
- ingress host and external address
- certificate readiness
- backend/frontend readiness
- HTTP status for / and /api/loans
```

## 4.2 AWS-only AI prompt template

Use this when the target platform is EKS.

```text
Deploy RiskManagement Pro to AWS EKS using managed PostgreSQL and existing repository scripts.

Requirements:
1) Use EKS as Kubernetes runtime.
2) Use Amazon RDS PostgreSQL (do not deploy in-cluster postgres).
3) Use ECR images provided as input.
4) Configure ingress + TLS for the provided domain.
5) Validate application health and login; keep fixing until successful.

Inputs:
- AWS region: <region>
- EKS cluster: <eksClusterName>
- Backend image: <accountId>.dkr.ecr.<region>.amazonaws.com/riskmanagement-backend:<backendTag>
- Frontend image: <accountId>.dkr.ecr.<region>.amazonaws.com/riskmanagement-frontend:<frontendTag>
- DB host: <rdsEndpoint>
- DB port: 5432
- DB name: <dbName>
- DB user: <dbUser>
- DB password: <dbPassword>
- Domain: <domain>

Execution path:
1) aws configure
2) aws eks update-kubeconfig --name <eksClusterName> --region <region>
3) Run scripts/helm-deploy-cloud-managed.ps1 with the provided values and -EnableTls

After deployment, print:
- current kubectl context
- ingress host and external address
- certificate readiness
- backend/frontend readiness
- HTTP status for / and /api/loans
```

## 5. Standard Deployment Paths

## 5.1 Cloud-neutral path (cluster already exists)

Run from repository root:

```powershell
.\scripts\helm-deploy-cloud-managed.ps1 `
  -Domain <domain> `
  -BackendRepository <backendRepository> `
  -BackendTag <backendTag> `
  -FrontendRepository <frontendRepository> `
  -FrontendTag <frontendTag> `
  -DbHost <dbHost> `
  -DbPort 5432 `
  -DbName <dbName> `
  -DbUser <dbUser> `
  -DbPassword <dbPassword> `
  -Namespace riskmanagement `
  -ReleaseName riskmanagement-cloud `
  -EnableTls
```

## 5.2 GCP one-command path (optional cluster creation)

```powershell
.\scripts\gcp-gke-deploy.ps1 `
  -ProjectId <projectId> `
  -Region <region> `
  -ClusterName <clusterName> `
  -CreateCluster `
  -Autopilot `
  -InstallIngressNginx `
  -Domain <domain> `
  -BackendRepository <backendRepository> `
  -BackendTag <backendTag> `
  -FrontendRepository <frontendRepository> `
  -FrontendTag <frontendTag> `
  -DbHost <dbHost> `
  -DbName <dbName> `
  -DbUser <dbUser> `
  -DbPassword <dbPassword> `
  -EnableTls
```

## 5.3 Azure AKS path (cluster exists)

Authenticate, pull cluster credentials, then run the cloud-neutral Helm deploy.

```powershell
az login
az account set --subscription <subscriptionId>
az aks get-credentials --resource-group <aksResourceGroup> --name <aksClusterName> --overwrite-existing

# Optional: verify context
kubectl config current-context

.\scripts\helm-deploy-cloud-managed.ps1 `
  -Domain <domain> `
  -BackendRepository <acrLoginServer>/<repo>/riskmanagement-backend `
  -BackendTag <backendTag> `
  -FrontendRepository <acrLoginServer>/<repo>/riskmanagement-frontend `
  -FrontendTag <frontendTag> `
  -DbHost <postgresFlexibleServerHost> `
  -DbPort 5432 `
  -DbName <dbName> `
  -DbUser <dbUser> `
  -DbPassword <dbPassword> `
  -Namespace riskmanagement `
  -ReleaseName riskmanagement-cloud `
  -EnableTls
```

Notes:

- Ensure AKS can resolve and reach PostgreSQL Flexible Server (private/public networking as configured).
- For private DB networking, run AKS and PostgreSQL in connected VNets.

## 5.4 AWS EKS path (cluster exists)

Authenticate, pull cluster credentials, then run the cloud-neutral Helm deploy.

```powershell
aws configure
aws eks update-kubeconfig --name <eksClusterName> --region <region>

# Optional: verify context
kubectl config current-context

.\scripts\helm-deploy-cloud-managed.ps1 `
  -Domain <domain> `
  -BackendRepository <accountId>.dkr.ecr.<region>.amazonaws.com/riskmanagement-backend `
  -BackendTag <backendTag> `
  -FrontendRepository <accountId>.dkr.ecr.<region>.amazonaws.com/riskmanagement-frontend `
  -FrontendTag <frontendTag> `
  -DbHost <rdsEndpoint> `
  -DbPort 5432 `
  -DbName <dbName> `
  -DbUser <dbUser> `
  -DbPassword <dbPassword> `
  -Namespace riskmanagement `
  -ReleaseName riskmanagement-cloud `
  -EnableTls
```

Notes:

- Ensure EKS node security groups can reach RDS on 5432.
- If RDS is private, place EKS and RDS in routable subnets/VPC configuration.

## 6. Post-Deployment Validation Checklist

Run these checks and require all pass:

```powershell
kubectl -n riskmanagement get deploy,pods,svc,ingress
kubectl -n riskmanagement get certificate,certificaterequest,order,challenge
curl -I https://<domain>/
curl -I https://<domain>/api/loans
```

Expected:

- backend deployment ready 2/2 (or desired replicas)
- frontend deployment ready 2/2 (or desired replicas)
- ingress has target host
- certificate Ready=True
- both curl calls return HTTP 200

## 6.1 Azure-specific validation commands

```powershell
az account show --query id -o tsv
kubectl config current-context
kubectl -n riskmanagement get ingress
kubectl -n riskmanagement get certificate
kubectl -n riskmanagement get pods -l app.kubernetes.io/component=backend
kubectl -n riskmanagement get pods -l app.kubernetes.io/component=frontend
curl -I https://<domain>/
curl -I https://<domain>/api/loans
```

## 6.2 AWS-specific validation commands

```powershell
aws sts get-caller-identity
kubectl config current-context
kubectl -n riskmanagement get ingress
kubectl -n riskmanagement get certificate
kubectl -n riskmanagement get pods -l app.kubernetes.io/component=backend
kubectl -n riskmanagement get pods -l app.kubernetes.io/component=frontend
curl -I https://<domain>/
curl -I https://<domain>/api/loans
```

## 6.3 AKS/EKS failure triage matrix

Use this table when deployment finishes but health checks fail.

| Symptom | Likely cause | Immediate checks | First fix to try |
|---|---|---|---|
| `ImagePullBackOff` on backend/frontend pods | Registry auth or image tag mismatch | `kubectl -n riskmanagement describe pod <pod-name>` | Verify image tag exists, then update release with correct `-BackendTag`/`-FrontendTag` and rerun deploy script |
| Backend pod `CrashLoopBackOff` with DB errors | DB host/user/password mismatch or network block to managed DB | `kubectl -n riskmanagement logs deploy/riskmanagement-cloud-backend --tail=200` | Confirm DB endpoint and credentials; ensure AKS/EKS network path to DB on 5432; redeploy with corrected DB values |
| Ingress has no external address | Ingress controller not installed or not ready | `kubectl get pods -A | Select-String ingress` and `kubectl -n riskmanagement get ingress` | Install/repair NGINX ingress controller, then recheck ingress status |
| TLS certificate not `Ready=True` | DNS not pointing to ingress IP or ACME challenge failing | `kubectl -n riskmanagement get certificate,certificaterequest,order,challenge` | Point DNS A record to ingress external IP, wait propagation, then recreate failed challenge/certificate |
| `curl` to `/api/loans` returns 401/403 unexpectedly | Auth/session flow issue or missing token path | `curl -I https://<domain>/api/loans` and browser login flow check | Confirm login with email/password first; validate frontend is calling `/api/auth/login` and then protected endpoints |
| Frontend loads but login fails | Wrong request payload or DB user records/password hash mismatch | Inspect browser network call to `/api/auth/login`; check backend auth logs | Ensure payload is `{email,password}` and verify user rows in `riskmanagement.users` |
| AKS cannot reach Azure PostgreSQL | VNet/private endpoint/NSG routing issue | AKS node subnet rules + DB firewall/private endpoint config | Allow AKS subnet to DB endpoint on 5432; fix VNet integration or firewall rules |
| EKS cannot reach RDS | Security group or subnet route issue | Check EKS node SG egress and RDS SG inbound on 5432 | Add SG rule to allow EKS node/group to RDS 5432; confirm same VPC/routable subnets |
| Helm release succeeded but old pods still serving | Rolling update stalled or old ReplicaSet still active | `kubectl -n riskmanagement get rs,pods` | `kubectl -n riskmanagement rollout restart deploy/riskmanagement-cloud-backend` and frontend deploy |
| Random 502/504 from ingress | Backend service endpoints empty or app startup delay | `kubectl -n riskmanagement get endpoints` and backend readiness events | Increase readiness initial delay/timeout in chart values and redeploy |

Fast recovery sequence:

1. `kubectl -n riskmanagement get pods,svc,ingress,certificate`
2. `kubectl -n riskmanagement logs deploy/riskmanagement-cloud-backend --tail=200`
3. Fix the first hard failure (image pull, DB connectivity, or DNS/TLS), then rerun `scripts/helm-deploy-cloud-managed.ps1` with corrected values.

## 6.4 Incident handoff checklist (copy/paste)

Use this template during incidents so the next engineer (or AI) can continue without losing context.

```text
Incident title:
Environment: AKS | EKS | GKE | Other
Timestamp (UTC):
Owner:

1) Current blast radius
- User impact:
- Affected URL(s):
- Affected APIs:

2) Failing signal
- First failing command:
- Error excerpt (exact):
- First observed at:

3) Layer isolation
- Failing layer: DNS | Ingress | TLS | Frontend | Backend | Database | Network
- Why this layer is suspected:

4) Evidence captured
- kubectl get pods,svc,ingress output:
- Certificate state:
- Backend logs (last 200 lines):
- Endpoint status checks (`/` and `/api/loans`):

5) Changes attempted
- Change 1:
- Result:
- Change 2:
- Result:

6) Final fix applied
- What was changed:
- Why it resolved the issue:
- Rollback command (if needed):

7) Post-fix validation
- kubectl readiness:
- HTTPS status for `/`:
- HTTPS status for `/api/loans`:
- Login flow validation result:

8) Follow-up actions
- Preventive action 1:
- Preventive action 2:
- Ticket/PR link:
```

## 6.5 Severity and escalation

Use these levels to classify incidents and drive response speed.

- SEV-1: Full outage of login or core APIs for all users, or widespread 5xx from ingress.
- SEV-2: Major degradation affecting a subset of users or one critical workflow.
- SEV-3: Minor degradation, workaround available, low user impact.

Escalation timeline:

1. SEV-1: Page on-call immediately, open incident bridge, update status every 15 minutes.
2. SEV-2: Notify on-call within 15 minutes, update status every 30 minutes.
3. SEV-3: Track in ticket queue, update status in normal sprint cadence.

Escalate to platform/cloud owner when:

- Network controls (SG/NSG/firewall/route tables) need change.
- DNS or certificate ownership is outside application team control.
- Cluster ingress controller or control-plane issues require admin privileges.

## 7. Authentication Notes

- Login API expects email + password (not username).
- If login fails after migrations/reseed, verify records in DB table riskmanagement.users.

## 8. Operational Commands

## 8.1 Check current Helm values

```powershell
helm get values riskmanagement-cloud -n riskmanagement -a
```

## 8.2 Roll back last release revision

```powershell
helm history riskmanagement-cloud -n riskmanagement
helm rollback riskmanagement-cloud <revision> -n riskmanagement
```

## 8.3 Remove deployment

```powershell
.\scripts\helm-destroy.ps1 -Namespace riskmanagement -ReleaseName riskmanagement-cloud
```

## 9. Cost and Security Minimums

- Restrict database authorized networks (avoid 0.0.0.0/0 in production).
- Rotate DB credentials and keep them in secret manager.
- Use private DB networking where possible.
- Enable backups and PITR for managed PostgreSQL.
- Enable WAF/rate limiting on ingress for internet traffic.

## 10. Handover Artifacts To Share

Share this list with the person or AI doing deployment:

- This file: AI_CLOUD_DEPLOYMENT_PLAYBOOK.md
- scripts/helm-deploy-cloud-managed.ps1
- scripts/gcp-gke-deploy.ps1
- helm/riskmanagement chart directory
- Final input values (Section 3)

With those artifacts, another AI agent can deploy this repo to a different cloud account with minimal manual work.
