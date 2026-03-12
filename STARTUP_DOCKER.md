# Docker Startup (Compose-Only)

Use this mode when you want backend, frontend, and PostgreSQL in containers.

## Prerequisites

1. Docker Desktop with Docker Compose v2

## Start services

Windows:

```powershell
.\scripts\docker-start.ps1
```

Linux/macOS:

```bash
./scripts/docker-start.sh
```

This starts:

1. Frontend at http://localhost
2. Backend at http://localhost:8080
3. PostgreSQL at localhost:5432

## Stop services

Windows:

```powershell
.\scripts\docker-stop.ps1
```

Linux/macOS:

```bash
./scripts/docker-stop.sh
```

## Notes

1. Do not run native scripts together with Docker scripts.
2. If startup fails, check container logs with `docker compose logs --tail=200`.
