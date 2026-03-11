# AI Local Execution Playbook (Windows + Docker)

This document is written for AI agents that can execute terminal commands.
Goal: install prerequisites if missing, initialize schema, run the app locally, and verify it is healthy.

## Expected Outcome

After this playbook completes:
- frontend responds at http://localhost
- backend responds at http://localhost:8080/api/loans
- PostgreSQL runs on localhost:5432
- schema riskmanagement exists

## Repository Root

Use this as working directory:

```powershell
Set-Location "c:\Users\raghu\Downloads\svr\riskmanagementv2"
```

## AI Operating Rules

1. Run each phase in order.
2. If a prerequisite is missing, install it and continue.
3. If install requires admin rights or reboot, report clearly and pause only for that action.
4. Do not skip verification checks.
5. If a command fails, capture error output and run the troubleshooting action for that phase.

## Phase 1: Check And Install Prerequisites

### 1.1 Verify tooling

```powershell
$ErrorActionPreference = "Stop"

Write-Host "Checking prerequisites..."

$hasWinget = [bool](Get-Command winget -ErrorAction SilentlyContinue)
if (-not $hasWinget) {
	Write-Host "winget not found. Install App Installer from Microsoft Store, then rerun." -ForegroundColor Yellow
	exit 1
}

$hasDocker = [bool](Get-Command docker -ErrorAction SilentlyContinue)
if (-not $hasDocker) {
	Write-Host "Docker CLI missing. Installing Docker Desktop..."
	winget install -e --id Docker.DockerDesktop --accept-package-agreements --accept-source-agreements
}

$hasGit = [bool](Get-Command git -ErrorAction SilentlyContinue)
if (-not $hasGit) {
	Write-Host "Git missing. Installing Git..."
	winget install -e --id Git.Git --accept-package-agreements --accept-source-agreements
}
```

### 1.2 Ensure Docker Desktop engine is running

```powershell
Write-Host "Starting Docker Desktop if needed..."
if (-not (Get-Process "Docker Desktop" -ErrorAction SilentlyContinue)) {
	Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"
}

Write-Host "Waiting for Docker engine..."
$max = 60
for ($i = 1; $i -le $max; $i++) {
	try {
		docker info | Out-Null
		Write-Host "Docker engine is ready."
		break
	} catch {
		Start-Sleep -Seconds 3
	}
	if ($i -eq $max) {
		Write-Host "Docker engine did not become ready in time." -ForegroundColor Red
		exit 1
	}
}

docker -v
docker compose version
```

## Phase 2: Start Application Containers

Use rebuild mode to avoid stale images:

```powershell
Set-Location "c:\Users\raghu\Downloads\svr\riskmanagementv2"
docker compose up -d --build
docker compose ps
```

Expected services:
- postgres
- backend
- frontend

## Phase 3: Ensure Database Schema Exists

The repository already mounts db/init.sql, but this step is idempotent and should still be executed.

```powershell
docker compose exec -T postgres psql -U postgres -d postgres -c "CREATE SCHEMA IF NOT EXISTS riskmanagement;"
```

Optional validation:

```powershell
docker compose exec -T postgres psql -U postgres -d postgres -c "SELECT schema_name FROM information_schema.schemata WHERE schema_name='riskmanagement';"
```

## Phase 4: Verify Runtime Health

```powershell
docker compose ps

$api = Invoke-WebRequest -Uri "http://localhost:8080/api/loans" -UseBasicParsing -TimeoutSec 30
$ui  = Invoke-WebRequest -Uri "http://localhost" -UseBasicParsing -TimeoutSec 30

"Backend status: $($api.StatusCode)"
"Frontend status: $($ui.StatusCode)"
```

Expected:
- Backend status: 200
- Frontend status: 200

## Phase 5: If Any Check Fails

Collect diagnostics:

```powershell
docker compose ps
docker compose logs --tail=200 postgres
docker compose logs --tail=200 backend
docker compose logs --tail=200 frontend
```

Then apply fixes:

### 5.1 Port conflicts

Required ports:
- 80
- 8080
- 5432

Find conflicting process on a port (example for 8080):

```powershell
netstat -ano | findstr :8080
```

### 5.2 Clean restart

```powershell
docker compose down -v
docker compose up -d --build
```

## Phase 6: Stop Commands

Stop containers only:

```powershell
docker compose down
```

Stop and remove data volume:

```powershell
docker compose down -v
```

## Share Images To Another Machine

After local build succeeds, export images:

```powershell
docker save -o riskmanagement-images.tar riskmanagementv2-backend:latest riskmanagementv2-frontend:latest postgres:15
```

Copy `riskmanagement-images.tar` and this repository (including `docker-compose.yml`) to another machine.

On the target machine, load images:

```powershell
docker load -i riskmanagement-images.tar
```

Then start:

```powershell
docker compose up -d
```

Health checks on target machine:

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/loans" -UseBasicParsing | Select-Object StatusCode
Invoke-WebRequest -Uri "http://localhost" -UseBasicParsing | Select-Object StatusCode
```

## Reusable Prompt For AI Agents

Use this prompt with an AI coding assistant that can run terminal commands:

```text
Read DOCKER_LOCAL_RUNBOOK.md and execute it end-to-end on Windows.
Install any missing prerequisites (Docker Desktop, Git) using winget.
Ensure Docker engine is running.
From repo root run docker compose up -d --build.
Ensure schema riskmanagement exists in postgres.
Verify frontend (http://localhost) and backend (http://localhost:8080/api/loans) both return HTTP 200.
If anything fails, collect compose logs and apply troubleshooting steps from the playbook until healthy.
Finally, report: prerequisite installs performed, container status, schema check result, endpoint status codes.
```

## One-Command Automation Script

This repository includes a script that executes the full flow automatically:

- script path: scripts/ai-bootstrap-local.ps1

Run from PowerShell:

```powershell
Set-Location "c:\Users\raghu\Downloads\svr\riskmanagementv2"
.\scripts\ai-bootstrap-local.ps1
```

If script execution is blocked by policy, run once per terminal session:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
```

Optional flags:

- `-SkipInstall` skips winget-based prerequisite installation checks.
- `-NoBuild` starts containers without rebuilding images.
- `-ResetData` runs `docker compose down -v` before startup.
- `-VerboseLogs` prints service logs automatically on failures.

Example with flags:

```powershell
.\scripts\ai-bootstrap-local.ps1 -SkipInstall -ResetData -VerboseLogs
```
