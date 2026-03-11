param(
  [string]$Namespace = "riskmanagement",
  [string]$ReleaseName = "riskmanagement"
)

$ErrorActionPreference = "Stop"

Write-Host "[INFO] Uninstalling release $ReleaseName from namespace $Namespace" -ForegroundColor Yellow
helm uninstall $ReleaseName -n $Namespace
