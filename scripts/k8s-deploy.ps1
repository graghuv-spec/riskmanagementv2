param(
  [ValidateSet("local", "cloud")]
  [string]$Overlay = "local",
  [string]$BackendImage = "",
  [string]$FrontendImage = ""
)

$ErrorActionPreference = "Stop"

function Info($msg) { Write-Host "[INFO] $msg" -ForegroundColor Green }
function Fail($msg) { Write-Host "[ERROR] $msg" -ForegroundColor Red; exit 1 }

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
  Fail "kubectl is not installed or not in PATH."
}

$overlayPath = "k8s/overlays/$Overlay"
Info "Applying overlay: $overlayPath"
kubectl apply -k $overlayPath

if ($BackendImage) {
  Info "Updating backend image to: $BackendImage"
  kubectl -n riskmanagement set image deployment/backend backend=$BackendImage
}

if ($FrontendImage) {
  Info "Updating frontend image to: $FrontendImage"
  kubectl -n riskmanagement set image deployment/frontend frontend=$FrontendImage
}

Info "Current rollout status"
kubectl -n riskmanagement rollout status deployment/postgres --timeout=180s
kubectl -n riskmanagement rollout status deployment/backend --timeout=300s
kubectl -n riskmanagement rollout status deployment/frontend --timeout=180s

Info "Services"
kubectl -n riskmanagement get svc
