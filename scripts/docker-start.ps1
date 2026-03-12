param(
  [switch]$Build
)

$ErrorActionPreference = "Stop"

function Info($msg) { Write-Host "[INFO] $msg" -ForegroundColor Green }
function Fail($msg) { Write-Host "[ERROR] $msg" -ForegroundColor Red; exit 1 }

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  Fail "docker not found. Install Docker Desktop."
}

Info "Docker startup mode (compose-only)"
if ($Build) {
  docker compose up -d --build
}
else {
  docker compose up -d
}

Info "Waiting for backend endpoint..."
$ready = $false
for ($i = 1; $i -le 30; $i++) {
  if (Test-NetConnection -ComputerName "localhost" -Port 8080 -InformationLevel Quiet -WarningAction SilentlyContinue) {
    $ready = $true
    break
  }
  Start-Sleep -Seconds 2
}

if (-not $ready) {
  Fail "Backend endpoint did not become healthy. Run: docker compose logs --tail=200 backend"
}

Write-Host "Frontend: http://localhost" -ForegroundColor Cyan
Write-Host "Backend : http://localhost:8080/api/loans" -ForegroundColor Cyan
