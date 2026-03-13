# Native Startup (No Docker)

Use this mode when Docker is not available.

## Prerequisites

1. Java 21+
2. Node 20+
3. PostgreSQL 15 running on localhost:5432
4. Optional: PostgreSQL client (`psql`) on PATH (enables pre-checks and auto schema creation)

## First-time setup

1. Ensure schema exists:

```sql
CREATE SCHEMA IF NOT EXISTS riskmanagement;
```

2. Update local DB credentials in backend/src/main/resources/application-local.yml if needed.

## Start services

Windows:

```powershell
.\scripts\native-start.ps1 -DbUser <db-user> -DbPassword <db-password>
```

Linux/macOS:

```bash
# Optional overrides when your DB is not default localhost/postgres
export LOCAL_DB_HOST=localhost
export LOCAL_DB_PORT=5432
export LOCAL_DB_NAME=postgres
export LOCAL_DB_USERNAME=postgres
export LOCAL_DB_PASSWORD=<db-password>
./scripts/native-start.sh
```

Backend local datasource values are read from:

1. `LOCAL_DB_URL` / `LOCAL_DB_USERNAME` / `LOCAL_DB_PASSWORD` (if set)
2. fallback defaults in `backend/src/main/resources/application-local.yml`

This starts:

1. Backend at http://localhost:8080 (Spring profile `local`)
2. Frontend at http://localhost:4200

## Stop services

Windows:

```powershell
.\scripts\native-stop.ps1
```

Linux/macOS:

```bash
./scripts/native-stop.sh
```

## Troubleshooting

1. PostgreSQL connection failed: confirm service is running and credentials in application-local.yml are correct.
2. Port conflict on 8080 or 4200: stop existing process, then rerun native-start script.
3. Backend startup errors: inspect logs/backend.log.
