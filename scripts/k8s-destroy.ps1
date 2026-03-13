$ErrorActionPreference = "Stop"

function Info($msg) { Write-Host "[INFO] $msg" -ForegroundColor Yellow }

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

Info "Deleting Kubernetes namespace riskmanagement"
kubectl delete namespace riskmanagement --ignore-not-found=true
