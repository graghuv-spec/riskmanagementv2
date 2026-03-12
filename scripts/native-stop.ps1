$ErrorActionPreference = "Stop"

function Info($msg) { Write-Host "[INFO] $msg" -ForegroundColor Green }

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$pidFile = Join-Path $repoRoot "logs\backend.pid"

if (Test-Path $pidFile) {
  $backendPid = (Get-Content $pidFile | Select-Object -First 1).Trim()
  if ($backendPid) {
    Stop-Process -Id ([int]$backendPid) -Force -ErrorAction SilentlyContinue
    Info "Stopped backend PID $backendPid"
  }
  Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
}
else {
  Info "Backend PID file not found. Backend is likely already stopped."
}

# Stop any stray dev-server process on 4200.
$frontendListen = Get-NetTCPConnection -State Listen -LocalPort 4200 -ErrorAction SilentlyContinue
if ($frontendListen) {
  Stop-Process -Id $frontendListen[0].OwningProcess -Force -ErrorAction SilentlyContinue
  Info "Stopped process listening on port 4200"
}
