param(
  [switch]$SkipInstall,
  [switch]$NoBuild,
  [switch]$ResetData,
  [switch]$VerboseLogs
)

$ErrorActionPreference = "Stop"

function Info($msg) { Write-Host "[INFO] $msg" -ForegroundColor Green }
function Warn($msg) { Write-Host "[WARN] $msg" -ForegroundColor Yellow }
function Fail($msg) { Write-Host "[ERROR] $msg" -ForegroundColor Red; exit 1 }

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

Info "Repository root: $repoRoot"

if (-not $SkipInstall) {
  Info "Checking prerequisites..."

  $hasWinget = [bool](Get-Command winget -ErrorAction SilentlyContinue)
  if (-not $hasWinget) {
    Fail "winget not found. Install App Installer from Microsoft Store and re-run this script."
  }

  $hasDocker = [bool](Get-Command docker -ErrorAction SilentlyContinue)
  if (-not $hasDocker) {
    Info "Installing Docker Desktop via winget..."
    winget install -e --id Docker.DockerDesktop --accept-package-agreements --accept-source-agreements
  }

  $hasGit = [bool](Get-Command git -ErrorAction SilentlyContinue)
  if (-not $hasGit) {
    Info "Installing Git via winget..."
    winget install -e --id Git.Git --accept-package-agreements --accept-source-agreements
  }
}

Info "Ensuring Docker Desktop is running..."
if (-not (Get-Process "Docker Desktop" -ErrorAction SilentlyContinue)) {
  $dockerDesktopPath = "C:\Program Files\Docker\Docker\Docker Desktop.exe"
  if (Test-Path $dockerDesktopPath) {
    Start-Process $dockerDesktopPath
  } else {
    Warn "Docker Desktop process not found and executable path missing."
    Warn "If Docker is installed in a custom path, start it manually now."
  }
}

Info "Waiting for Docker engine readiness..."
$maxAttempts = 60
for ($i = 1; $i -le $maxAttempts; $i++) {
  try {
    docker info | Out-Null
    Info "Docker engine is ready."
    break
  } catch {
    Start-Sleep -Seconds 3
  }

  if ($i -eq $maxAttempts) {
    Fail "Docker engine did not become ready in time."
  }
}

Info "Docker versions:"
docker -v
docker compose version

if ($ResetData) {
  Warn "ResetData enabled: removing containers and volumes..."
  docker compose down -v
}

if ($NoBuild) {
  Info "Starting stack without rebuild..."
  docker compose up -d
} else {
  Info "Starting stack with rebuild..."
  docker compose up -d --build
}

Info "Ensuring schema riskmanagement exists..."
docker compose exec -T postgres psql -U postgres -d postgres -c "CREATE SCHEMA IF NOT EXISTS riskmanagement;" | Out-Null

Info "Waiting for backend health check..."
$backendOk = $false
for ($i = 1; $i -le 30; $i++) {
  try {
    $apiResp = Invoke-WebRequest -Uri "http://localhost:8080/api/loans" -UseBasicParsing -TimeoutSec 15
    if ($apiResp.StatusCode -eq 200) {
      $backendOk = $true
      break
    }
  } catch {
    Start-Sleep -Seconds 2
  }
}
if (-not $backendOk) {
  if ($VerboseLogs) {
    docker compose logs --tail=200 backend
  }
  Fail "Backend did not become healthy on http://localhost:8080/api/loans"
}

Info "Checking frontend response..."
try {
  $uiResp = Invoke-WebRequest -Uri "http://localhost" -UseBasicParsing -TimeoutSec 20
} catch {
  if ($VerboseLogs) {
    docker compose logs --tail=200 frontend
  }
  Fail "Frontend did not respond on http://localhost"
}

if ($uiResp.StatusCode -ne 200) {
  Fail "Frontend returned unexpected status code: $($uiResp.StatusCode)"
}

Info "Container status:"
docker compose ps

Write-Host ""
Write-Host "Local environment is ready." -ForegroundColor Cyan
Write-Host "Frontend: http://localhost" -ForegroundColor Cyan
Write-Host "Backend:  http://localhost:8080/api/loans" -ForegroundColor Cyan
Write-Host ""
Write-Host "Stop with: docker compose down" -ForegroundColor Cyan
