param(
  [string]$DbHost = "localhost",
  [int]$DbPort = 5432,
  [string]$DbName = "postgres",
  [string]$DbUser = "postgres",
  [string]$DbPassword = "",
  [string]$Schema = "riskmanagement"
)

$ErrorActionPreference = "Stop"

function Info($msg) { Write-Host "[INFO] $msg" -ForegroundColor Green }
function Fail($msg) { Write-Host "[ERROR] $msg" -ForegroundColor Red; exit 1 }

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$logDir = Join-Path $repoRoot "logs"
$backendLog = Join-Path $logDir "backend.log"
$backendPidFile = Join-Path $logDir "backend.pid"

Set-Location $repoRoot

Info "Native startup mode (no Docker)"

if (-not (Get-Command java -ErrorAction SilentlyContinue)) { Fail "Java not found. Install JDK 21+." }
if (-not (Get-Command node -ErrorAction SilentlyContinue)) { Fail "Node.js not found. Install Node 20+." }
if (-not (Get-Command npm -ErrorAction SilentlyContinue)) { Fail "npm not found." }
$hasPsql = [bool](Get-Command psql -ErrorAction SilentlyContinue)

# Ensure ports are available before starting services.
$ports = @(8080, 4200)
foreach ($port in $ports) {
  $inUse = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue
  if ($inUse) {
    $portPid = $inUse[0].OwningProcess
    Stop-Process -Id $portPid -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
  }
}

if (-not (Test-Path $logDir)) {
  New-Item -Path $logDir -ItemType Directory | Out-Null
}

if ($hasPsql) {
  Info "Checking PostgreSQL connectivity on $DbHost`:$DbPort..."
  $env:PGPASSWORD = $DbPassword
  $null = & psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -c "SELECT 1;" 2>$null
  if ($LASTEXITCODE -ne 0) {
    Fail "Cannot connect to PostgreSQL at $DbHost`:$DbPort/$DbName as user '$DbUser'."
  }

  Info "Ensuring schema $Schema exists..."
  $null = & psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -c "CREATE SCHEMA IF NOT EXISTS $Schema;" 2>$null
  if ($LASTEXITCODE -ne 0) {
    Fail "Failed to create or validate schema '$Schema'."
  }
}
else {
  Write-Host "[WARN] psql not found. Skipping DB pre-checks; backend health check will validate connectivity." -ForegroundColor Yellow
}

$env:LOCAL_DB_HOST = $DbHost
$env:LOCAL_DB_PORT = "$DbPort"
$env:LOCAL_DB_NAME = $DbName
$env:LOCAL_DB_USERNAME = $DbUser
$env:LOCAL_DB_SCHEMA = $Schema
if ($DbPassword) {
  $env:LOCAL_DB_PASSWORD = $DbPassword
}

Info "Starting backend with Spring profile local..."
Set-Location (Join-Path $repoRoot "backend")
$gradleCommand = ".\gradlew.bat bootRun --no-daemon --args=--spring.profiles.active=local > `"$backendLog`" 2>&1"
$backendProc = Start-Process -FilePath "cmd.exe" -ArgumentList @("/c", $gradleCommand) -PassThru
$backendProc.Id | Set-Content -Path $backendPidFile -Encoding ascii

Set-Location $repoRoot
Info "Waiting for backend health endpoint..."
$backendReady = $false
for ($i = 1; $i -le 30; $i++) {
  if (Test-NetConnection -ComputerName "localhost" -Port 8080 -InformationLevel Quiet -WarningAction SilentlyContinue) {
    $backendReady = $true
    break
  }
  Start-Sleep -Seconds 2
}

if (-not $backendReady) {
  Fail "Backend did not become healthy. Check logs/backend.log"
}

if (-not (Test-Path (Join-Path $repoRoot "frontend\node_modules"))) {
  Info "Installing frontend dependencies..."
  Set-Location (Join-Path $repoRoot "frontend")
  npm install
}

Info "Starting frontend dev server on http://localhost:4200"
Set-Location (Join-Path $repoRoot "frontend")
npm start
