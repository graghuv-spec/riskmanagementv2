# RiskManagement Pro

Enterprise risk assessment platform for microfinance institutions — Java 21 Spring Boot backend, PostgreSQL, GraphQL API, and Angular 17 frontend.

Docker-only local setup guide: see `DOCKER_LOCAL_RUNBOOK.md`.

Native local startup guide (no Docker): see `STARTUP_NATIVE.md`.

Docker local startup guide (compose-only): see `STARTUP_DOCKER.md`.

Easy command quick reference (local + docker start/stop): see `EASY_COMMANDS.md`.

Kubernetes three-tier deployment guide: see `KUBERNETES_DEPLOYMENT.md`.

AI-ready cloud deployment handoff guide (GCP/Azure/AWS): see `AI_CLOUD_DEPLOYMENT_PLAYBOOK.md`.

## Choose one startup mode

### Native mode (no Docker)

Use this when developers do not have Docker. Backend, frontend, and PostgreSQL run on the host.

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

### Docker mode (compose-only)

Use this when you want all services in containers.

Windows:

```powershell
.\scripts\docker-start.ps1
```

Linux/macOS:

```bash
./scripts/docker-start.sh
```

### Easy shell helper (Linux/macOS/Git Bash)

Use one script for all start/stop actions:

```bash
./easy.sh local-start
./easy.sh local-stop
./easy.sh docker-start
./easy.sh docker-stop
```

Kubernetes deploy script (local overlay):

```powershell
.\scripts\k8s-deploy.ps1 -Overlay local
```

Helm deploy (recommended for multi-environment):

```powershell
.\scripts\helm-deploy.ps1 -Environment local
```

Cloud deploy with managed DB override:

```powershell
.\scripts\helm-deploy-cloud-managed.ps1 -Domain app.example.com -BackendRepository <registry>/riskmanagement-backend -FrontendRepository <registry>/riskmanagement-frontend -DbHost <managed-db-host> -DbUser <db-user> -DbPassword <db-password>
```

Google Cloud GKE deploy:

```powershell
.\scripts\gcp-gke-deploy.ps1 -ProjectId <project-id> -Region <region> -ClusterName riskmanagement-gke -CreateCluster -Autopilot -InstallIngressNginx -Domain app.example.com -BackendRepository <registry>/riskmanagement-backend -FrontendRepository <registry>/riskmanagement-frontend -DbHost <managed-db-host> -DbUser <db-user> -DbPassword <db-password> -EnableTls
```

---

## Table of Contents

- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Startup Modes](#startup-modes)
- [Database Setup](#database-setup)
- [Local Configuration](#local-configuration)
- [Backend](#backend)
- [Frontend](#frontend)
- [Running Tests](#running-tests)
- [Key URLs](#key-urls)
- [Demo Credentials](#demo-credentials)
- [Troubleshooting](#troubleshooting)

---

## Architecture

| Layer      | Technology                              |
|------------|-----------------------------------------|
| Backend    | Java 21, Spring Boot 3.4.0, Gradle      |
| API        | REST + GraphQL (Spring for GraphQL)     |
| Database   | PostgreSQL 15, schema `riskmanagement`  |
| Frontend   | Angular 17, TypeScript 5.2, Chart.js 4 |
| E2E Tests  | Playwright 1.42 (Chromium)              |
| Unit Tests | JUnit 5 + Spring Boot Test (backend), Karma/Jasmine (frontend) |

---

## Prerequisites

Install all of the following before proceeding.

### 1. Java 21

The backend requires **Java 21** (LTS).

- Download: https://adoptium.net
  Choose **Temurin 21 (LTS)**.

Verify:
```bash
java -version
# openjdk version "21.x.x" ...
```

> Gradle is bundled via the Gradle Wrapper (`./gradlew`). You do **not** need to install Gradle separately.

---

### 2. Node.js 20 and npm

The frontend and E2E tests require **Node 20** (LTS).

- Download: https://nodejs.org (choose LTS v20)

Verify:
```bash
node -v   # v20.x.x
npm -v    # 10.x.x
```

---

### 3. Angular CLI 17

```bash
npm install -g @angular/cli@17
ng version   # Angular CLI: 17.x.x
```

---

### 4. PostgreSQL 15 (required for native mode)

Native mode requires a local PostgreSQL instance running on localhost:5432.

Verify:
```bash
psql --version
```

### 5. Docker Desktop (optional, Docker mode only)

Install Docker only if you plan to use compose startup mode.

- Download: https://www.docker.com/products/docker-desktop

Verify:
```bash
docker -v
docker compose version
```

### 6. Git

```bash
git --version   # git version 2.x.x
```

---

## Startup Modes

Pick one mode and do not mix commands between modes.

### Mode A: Native (no Docker)

Requirements:

1. Java 21+
2. Node 20+
3. PostgreSQL running locally

Windows:

```powershell
.\scripts\native-start.ps1
```

Linux/macOS:

```bash
./scripts/native-start.sh
```

Stop native mode:

Windows:

```powershell
.\scripts\native-stop.ps1
```

Linux/macOS:

```bash
./scripts/native-stop.sh
```

### Mode B: Docker (compose-only)

Requirements:

1. Docker Desktop

Windows:

```powershell
.\scripts\docker-start.ps1
```

Linux/macOS:

```bash
./scripts/docker-start.sh
```

Stop Docker mode:

Windows:

```powershell
.\scripts\docker-stop.ps1
```

Linux/macOS:

```bash
./scripts/docker-stop.sh
```

Open http://localhost:4200 for native mode, or http://localhost for Docker mode.

---

## Database Setup

### Option A — Native mode local PostgreSQL (recommended)

For strict no-Docker startup, install PostgreSQL locally and ensure it is running.

```sql
CREATE SCHEMA IF NOT EXISTS riskmanagement;
```

Default local connection values:

| Setting           | Value      |
|-------------------|------------|
| Host              | localhost  |
| Port              | 5432       |
| Database          | postgres   |
| Username          | postgres   |
| Password          | admin      |

Then run native mode scripts from the Startup Modes section.

---

### Option B — Docker Compose

A `docker-compose.yml` at the project root starts a PostgreSQL 15 instance with persistent storage.

```bash
# Start in background
docker compose up -d

# Check it is healthy
docker compose ps
```

The container exposes PostgreSQL on **localhost:5432**.

| Setting           | Value      |
|-------------------|------------|
| Host              | localhost  |
| Port              | 5432       |
| Database          | postgres   |
| Username          | postgres   |
| Password          | admin      |

On first `bootRun` Spring Boot will:
1. Create the `riskmanagement` schema automatically (via `ddl-auto: update`).
2. Seed demo data (10 borrowers, 10 loans, risk scores, 2 users) through `DataInitializer`.

To stop and keep data:
```bash
docker compose stop
```

To destroy the container **and all data**:
```bash
docker compose down -v
```

---

Docker mode is optional and fully separate from native mode.

---

## Local Configuration

`backend/src/main/resources/application-local.yml` is your personal override file.
It is listed in `.gitignore` and is **never committed**.

Spring Boot automatically loads it with higher priority than `application.yml`.

Edit the file to match your local DB:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/postgres?stringtype=unspecified&sslmode=disable&currentSchema=riskmanagement
    username: postgres
    password: admin          # change to your actual password

  jpa:
    properties:
      hibernate:
        default_schema: riskmanagement   # change if you use a different schema name
```

All other settings (`ddl-auto`, logging, pool size) are inherited from `application.yml`.

Native startup scripts also support these environment variables:

- `LOCAL_DB_HOST`
- `LOCAL_DB_PORT`
- `LOCAL_DB_NAME`
- `LOCAL_DB_SCHEMA`
- `LOCAL_DB_USERNAME`
- `LOCAL_DB_PASSWORD`

### Production Profile (deterministic cloud runtime)

Cloud deployments should run with Spring profile `prod`.

The Helm chart now sets `SPRING_PROFILES_ACTIVE` via values:

- Base values: `backend.env.springProfilesActive: local`
- Cloud values: `backend.env.springProfilesActive: prod`

Required production env vars:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `ALLOWED_ORIGINS` (comma-separated)
- `JWT_SECRET` (minimum 32 characters recommended)

Optional production env vars:

- `JWT_TTL_HOURS` (default `8`)

---

## Backend

### Run in development mode

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

The server starts on **http://localhost:8080**.

### Build JAR (without running)

```bash
cd backend
./gradlew build --no-daemon
# output: backend/build/libs/backend-0.0.1-SNAPSHOT.jar
```

### Run the built JAR

```bash
java -jar backend/build/libs/backend-0.0.1-SNAPSHOT.jar
```

---

## Frontend

### Install dependencies

```bash
cd frontend
npm install
```

> Run this once after cloning, and again whenever `package.json` changes.

### Start development server

```bash
npm start
# or: ng serve
```

Frontend is available at **http://localhost:4200**.
The dev server proxies API calls to `localhost:8080` (backend must be running).

### Production build

```bash
npm run build:prod
# output: frontend/dist/
```

---

## Running Tests

### Backend — Unit + Integration tests

```bash
cd backend
./gradlew test --no-daemon
```

- Uses the `test` Spring profile (`application-test.yml`).
- Requires PostgreSQL to be running (Docker Compose or local).
- Tests run against schema `riskmanagement_test` (isolated from the main schema).
- Schema is created and dropped automatically (`ddl-auto: create-drop`).
- HTML report: `backend/build/reports/tests/test/index.html`

### Frontend — Unit tests (Karma / Jasmine)

```bash
cd frontend
npm test             # interactive (Chrome)
npm run test:ci      # headless Chrome (for CI)
```

### E2E tests — Playwright (Chromium)

Both backend and Angular dev server must be running before executing E2E tests.

```bash
# Terminal 1 — backend
cd backend && ./gradlew bootRun

# Terminal 2 — frontend
cd frontend && npm start

# Terminal 3 — run E2E tests
cd frontend
npm run e2e
```

Other Playwright commands:

```bash
npm run e2e:ui       # open Playwright interactive UI
npm run e2e:debug    # step through tests in debug mode
npm run e2e:report   # open the last HTML report
```

Reports and traces are saved to:
- `frontend/playwright-report/`
- `frontend/test-results/` (on failure)

---

## Key URLs

| URL                               | Description                        |
|-----------------------------------|------------------------------------|
| http://localhost:4200             | Angular application                |
| http://localhost:8080             | Spring Boot REST API               |
| http://localhost:8080/graphql     | GraphQL endpoint                   |
| http://localhost:8080/graphiql    | GraphiQL interactive playground    |
| http://localhost:8080/api/loans   | Loans REST endpoint                |
| http://localhost:8080/api/users   | Users REST endpoint                |
| http://localhost:8080/api/borrowers | Borrowers REST endpoint          |

---

## Demo Credentials

Seeded automatically on the first backend start (via `DataInitializer`).

| Role          | Email                    | Password      |
|---------------|--------------------------|---------------|
| Admin         | admin@mfb.com            | password123   |
| Loan Officer  | loan.officer@mfb.com     | password123   |

---

## Troubleshooting

### `./gradlew: Permission denied`
```bash
chmod +x backend/gradlew
```

### `Connection refused` on port 5432
Make sure Docker is running and the container is healthy:
```bash
docker compose up -d
docker compose ps   # Status should be "healthy"
```

### `npm ci` fails — missing `package-lock.json` entries
Run `npm install` (not `npm ci`) once locally to regenerate the lockfile, then commit it:
```bash
cd frontend
npm install
git add package-lock.json
git commit -m "chore: update package-lock after adding playwright"
```

### Schema does not exist
Connect to PostgreSQL and create it manually:
```bash
PGPASSWORD=admin psql -h localhost -U postgres -c "CREATE SCHEMA IF NOT EXISTS riskmanagement;"
```

### Port 8080 already in use
```bash
# Find the process using the port
lsof -i :8080      # macOS/Linux
netstat -ano | findstr :8080  # Windows

# Kill it, or change the backend port in application-local.yml:
# server:
#   port: 8081
```

### Angular CLI not found
```bash
npm install -g @angular/cli@17
```

### Java version mismatch
Ensure `JAVA_HOME` points to a Java 21 JDK:
```bash
java -version        # must show 21
echo $JAVA_HOME      # macOS/Linux
echo %JAVA_HOME%     # Windows
```
