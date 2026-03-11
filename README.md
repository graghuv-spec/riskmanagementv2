# RiskManagement Pro

Enterprise risk assessment platform for microfinance institutions — Java 21 Spring Boot backend, PostgreSQL, GraphQL API, and Angular 17 frontend.

Docker-only local setup guide: see `DOCKER_LOCAL_RUNBOOK.md`.

One-command local bootstrap (PowerShell):

```powershell
.\scripts\ai-bootstrap-local.ps1
```

---

## Table of Contents

- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
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

### 4. Docker Desktop

Used to run PostgreSQL locally via Docker Compose.

- Download: https://www.docker.com/products/docker-desktop

Verify:
```bash
docker -v          # Docker version 24+
docker compose version   # Docker Compose version v2+
```

---

### 5. Git

```bash
git --version   # git version 2.x.x
```

---

## Quick Start

```bash
# 1. Clone the repository
git clone <repo-url>
cd riskmanagementv2

# 2. Start PostgreSQL
docker compose up -d

# 3. Configure local credentials (first time only)
#    Edit backend/src/main/resources/application-local.yml
#    (see Local Configuration section)

# 4. Start backend  (new terminal)
cd backend
./gradlew bootRun

# 5. Install frontend deps and start  (new terminal)
cd frontend
npm install
npm start
```

Open http://localhost:4200 in your browser.

---

## Database Setup

### Option A — Docker Compose (recommended)

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

### Option B — Existing Local PostgreSQL

If you already have PostgreSQL running locally, skip Docker and just create the schema:

```sql
CREATE SCHEMA IF NOT EXISTS riskmanagement;
```

Then update `application-local.yml` with your host/port/password (see next section).

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

---

## Backend

### Run in development mode

```bash
cd backend
./gradlew bootRun
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
