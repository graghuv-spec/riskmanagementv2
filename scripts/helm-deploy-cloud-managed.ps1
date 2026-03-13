param(
  [Parameter(Mandatory = $true)]
  [string]$Domain,
  [Parameter(Mandatory = $true)]
  [string]$BackendRepository,
  [string]$BackendTag = "latest",
  [Parameter(Mandatory = $true)]
  [string]$FrontendRepository,
  [string]$FrontendTag = "latest",
  [Parameter(Mandatory = $true)]
  [string]$DbHost,
  [int]$DbPort = 5432,
  [string]$DbName = "postgres",
  [Parameter(Mandatory = $true)]
  [string]$DbUser,
  [Parameter(Mandatory = $true)]
  [string]$DbPassword,
  [string]$Namespace = "riskmanagement",
  [string]$ReleaseName = "riskmanagement-cloud",
  [string]$DbSecretName = "riskmanagement-db-credentials",
  [string]$DbUsernameKey = "POSTGRES_USER",
  [string]$DbPasswordKey = "POSTGRES_PASSWORD",
  [string]$IngressClassName = "nginx",
  [string]$TlsSecretName = "riskmanagement-tls",
  [string]$ClusterIssuer = "letsencrypt-prod",
  [switch]$EnableTls
)

$ErrorActionPreference = "Stop"

function Info($msg) { Write-Host "[INFO] $msg" -ForegroundColor Green }
function Fail($msg) { Write-Host "[ERROR] $msg" -ForegroundColor Red; exit 1 }

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

if (-not (Get-Command helm -ErrorAction SilentlyContinue)) {
  Fail "helm is not installed or not in PATH."
}
if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
  Fail "kubectl is not installed or not in PATH."
}

$tmpDir = Join-Path $repoRoot "tmp"
New-Item -ItemType Directory -Path $tmpDir -Force | Out-Null
$overrideFile = Join-Path $tmpDir "values-cloud-managed.auto.yaml"

$allowedOrigins = "https://$Domain"
$dbUrl = "jdbc:postgresql://${DbHost}:${DbPort}/${DbName}?sslmode=require&currentSchema=riskmanagement"
$backendUrl = "http://${ReleaseName}-riskmanagement-backend.${Namespace}.svc.cluster.local:8080"

$tlsBlock = "  tls: []"
$certManagerAnnotation = ""
if ($EnableTls) {
  $certManagerAnnotation = "`n    cert-manager.io/cluster-issuer: $ClusterIssuer"
  $tlsBlock = @"
  tls:
    - secretName: $TlsSecretName
      hosts:
        - $Domain
"@
}

$overrideYaml = @"
postgres:
  enabled: false

backend:
  image:
    repository: $BackendRepository
    tag: "$BackendTag"
  dbCredentials:
    existingSecretName: $DbSecretName
    usernameKey: $DbUsernameKey
    passwordKey: $DbPasswordKey
  env:
    dbUrl: $dbUrl
    allowedOrigins: $allowedOrigins
    seedEnabled: "false"
    seedResetBeforeSeed: "false"

frontend:
  image:
    repository: $FrontendRepository
    tag: "$FrontendTag"
  env:
    backendUrl: $backendUrl

ingress:
  enabled: true
  className: $IngressClassName
  annotations:
    nginx.ingress.kubernetes.io/proxy-read-timeout: "60"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "60"$certManagerAnnotation
  hosts:
    - host: $Domain
      paths:
        - path: /
          pathType: Prefix
$tlsBlock
"@

Set-Content -Path $overrideFile -Value $overrideYaml -Encoding ASCII
Info "Generated override values at $overrideFile"

Info "Ensuring namespace '$Namespace' exists"
kubectl create namespace $Namespace --dry-run=client -o yaml | kubectl apply -f - | Out-Null

Info "Creating/updating managed DB credentials secret '$DbSecretName'"
kubectl -n $Namespace create secret generic $DbSecretName --from-literal=$DbUsernameKey=$DbUser --from-literal=$DbPasswordKey=$DbPassword --dry-run=client -o yaml | kubectl apply -f - | Out-Null

Info "Deploying Helm release '$ReleaseName'"
.\scripts\helm-deploy.ps1 -Environment cloud -Namespace $Namespace -ReleaseName $ReleaseName -ExtraValuesFile $overrideFile -ManagedDatabase

Info "Ingress"
kubectl -n $Namespace get ingress

Info "Done. If DNS is ready, open: https://$Domain (or http://$Domain if TLS disabled)."
