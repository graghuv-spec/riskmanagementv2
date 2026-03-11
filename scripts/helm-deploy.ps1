param(
  [ValidateSet("local", "cloud")]
  [string]$Environment = "local",
  [string]$Namespace = "riskmanagement",
  [string]$ReleaseName = "riskmanagement"
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

$valuesFile = "helm/riskmanagement/values-$Environment.yaml"
if (-not (Test-Path $valuesFile)) {
  Fail "Values file not found: $valuesFile"
}

Info "Deploying release '$ReleaseName' to namespace '$Namespace' with environment '$Environment'"
helm upgrade --install $ReleaseName helm/riskmanagement -n $Namespace --create-namespace -f helm/riskmanagement/values.yaml -f $valuesFile

Info "Waiting for tier rollouts"
kubectl -n $Namespace rollout status deployment/$ReleaseName-riskmanagement-postgres --timeout=180s
kubectl -n $Namespace rollout status deployment/$ReleaseName-riskmanagement-backend --timeout=300s
kubectl -n $Namespace rollout status deployment/$ReleaseName-riskmanagement-frontend --timeout=180s

Info "Services"
kubectl -n $Namespace get svc
