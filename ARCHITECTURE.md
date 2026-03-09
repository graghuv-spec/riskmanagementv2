# Architecture & Project Structure

---

## Table of Contents

- [System Architecture](#system-architecture)
- [Component Interaction](#component-interaction)
- [Request Flow — Risk Score Calculation](#request-flow--risk-score-calculation)
- [Authentication Flow](#authentication-flow)
- [Database Schema](#database-schema)
- [CI/CD Pipeline](#cicd-pipeline)
- [Project Structure](#project-structure)
- [Backend Layers](#backend-layers)
- [Frontend Layers](#frontend-layers)
- [API Reference](#api-reference)

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             BROWSER / CLIENT                                │
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                    Angular 17 SPA  :4200                           │   │
│   │                                                                     │   │
│   │  ┌──────────┐  ┌──────────┐  ┌────────────┐  ┌─────────────────┐  │   │
│   │  │  Login   │  │Dashboard │  │  New Loan  │  │ Risk Result     │  │   │
│   │  └──────────┘  └──────────┘  └────────────┘  └─────────────────┘  │   │
│   │  ┌──────────────────────┐  ┌──────────────┐  ┌─────────────────┐  │   │
│   │  │ Portfolio Analytics  │  │   Reports    │  │ Header/Sidebar  │  │   │
│   │  └──────────────────────┘  └──────────────┘  └─────────────────┘  │   │
│   │                                                                     │   │
│   │  ┌──────────────────────────────────────────────────────────────┐  │   │
│   │  │  Core Services: AuthService · LoanService · PortfolioService │  │   │
│   │  │  Auth Guard  │  localStorage session  │  HTTP Client         │  │   │
│   │  └──────────────────────────────────────────────────────────────┘  │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                          │ HTTP / JSON                                       │
└──────────────────────────┼──────────────────────────────────────────────────┘
                           │
          ┌────────────────┴─────────────────┐
          │         REST  /api/**             │
          │         GraphQL  /graphql         │
          │         GraphiQL /graphiql        │
          ▼                                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Spring Boot 3.4.0  :8080                                 │
│                                                                             │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │  Controllers (REST)                                                    │ │
│  │  AuthController · LoanController · BorrowerController                 │ │
│  │  UserController · RiskScoreController · RepaymentController           │ │
│  │  PortfolioMetricsController · InstitutionController                   │ │
│  ├────────────────────────────────────────────────────────────────────────┤ │
│  │  Controllers (GraphQL)                                                 │ │
│  │  LoanGraphQLController  (Query: loans, loan  |  Mutation: CRUD)       │ │
│  ├────────────────────────────────────────────────────────────────────────┤ │
│  │  Services                                                              │ │
│  │  LoanService · BorrowerService · UserService · RiskScoreService       │ │
│  │  RiskCalculationService · RepaymentService · AuditLogService          │ │
│  │  PortfolioMetricsService · InstitutionService                         │ │
│  ├────────────────────────────────────────────────────────────────────────┤ │
│  │  Repositories (Spring Data JPA)                                        │ │
│  │  LoanRepository · BorrowerRepository · UserRepository                 │ │
│  │  RiskScoreRepository · RepaymentRepository · AuditLogRepository       │ │
│  │  PortfolioMetricsRepository · InstitutionRepository                   │ │
│  ├────────────────────────────────────────────────────────────────────────┤ │
│  │  Config                                                                │ │
│  │  CorsConfig  ·  DataInitializer (@Profile !test)                      │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                          │ JDBC / HikariCP                                  │
└──────────────────────────┼──────────────────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────────────────┐
│                   PostgreSQL 15  :5432                                      │
│                                                                             │
│    Schema: riskmanagement          Schema: riskmanagement_test              │
│    ┌──────────────────────┐        ┌────────────────────────────┐           │
│    │ institutions         │        │ (mirror of riskmanagement) │           │
│    │ users                │        │ ddl-auto: create-drop      │           │
│    │ borrowers            │        │ used by integration tests  │           │
│    │ loans                │        └────────────────────────────┘           │
│    │ repayments           │                                                 │
│    │ risk_scores          │                                                 │
│    │ portfolio_metrics    │                                                 │
│    │ audit_logs           │                                                 │
│    └──────────────────────┘                                                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Component Interaction

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Angular Frontend                                                           │
│                                                                             │
│  NewLoanComponent                                                           │
│       │  collects borrower + loan fields                                    │
│       │                                                                     │
│       ▼                                                                     │
│  LoanService.calculateRiskScore()  ──────────────────────────────────────┐  │
│                                  POST /api/risk-scores/calculate          │  │
└───────────────────────────────────────────────────────────────────────────┼──┘
                                                                            │
                                              ┌─────────────────────────────▼──┐
                                              │  RiskScoreController           │
                                              │  RiskCalculationService        │
                                              │    ├─ incomeScore              │
                                              │    ├─ repaymentScore           │
                                              │    │    └─ RepaymentRepository  │
                                              │    │       .findByLoanId()      │
                                              │    ├─ collateralScore          │
                                              │    ├─ sectorScore              │
                                              │    └─ locationScore            │
                                              │  → RiskScore entity            │
                                              └────────────────────────────────┘
                                                            │
                                              ┌─────────────▼──────────────────┐
                                              │  RiskResultComponent           │
                                              │    displays: score, grade,     │
                                              │    PD, credit limit, drivers   │
                                              └────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────────┐
│  DashboardComponent                                                           │
│       │                                                                       │
│       ├── PortfolioService.getDashboardData()                                 │
│       │       └─ forkJoin(loans, borrowers, riskScores, metrics)              │
│       │             GET /api/loans                                            │
│       │             GET /api/borrowers                                        │
│       │             GET /api/risk-scores                                      │
│       │             GET /api/portfolio-metrics                                │
│       │                                                                       │
│       └── renders: KPI cards · portfolio value · Chart.js charts             │
│                                                                               │
│  PortfolioAnalyticsComponent                                                  │
│       └─ PortfolioService.getPortfolioData()                                  │
│               └─ forkJoin(loans, borrowers, riskScores, repayments)           │
│                                                                               │
│  ReportGeneratorComponent                                                     │
│       └─ generates PDF via jsPDF + html2canvas                                │
└───────────────────────────────────────────────────────────────────────────────┘
```

---

## Request Flow — Risk Score Calculation

```
Browser
  │
  │  1. User fills New Loan form
  │
  ▼
NewLoanComponent (Angular)
  │
  │  2. POST /api/risk-scores/calculate
  │     { fullName, nationalId, gender, age, location, businessSector,
  │       monthlyIncome, collateralValue, loanAmount, interestRate,
  │       tenureMonths, status }
  │
  ▼
RiskScoreController.calculateRisk()
  │
  │  3. delegates to
  ▼
RiskCalculationService.calculateRiskScore()
  │
  ├──── incomeScore     = f(monthlyIncome)            weight: 25%
  ├──── repaymentScore  = f(daysPastDue per loan)     weight: 30%
  │         └── RepaymentRepository.findByLoanId()
  ├──── collateralScore = f(collateral / loanAmount)  weight: 20%
  ├──── sectorScore     = f(businessSector)           weight: 15%
  └──── locationScore   = f(location)                 weight: 10%
  │
  │  4. returns RiskScore { score, grade, probabilityOfDefault,
  │                         recommendedCreditLimit, calculationDetails }
  ▼
HTTP 200 JSON
  │
  ▼
Angular router.navigate(['/risk-result'])
  │
  ▼
RiskResultComponent — displays result with Chart.js breakdown
```

---

## Authentication Flow

```
Browser
  │
  │  1. User enters email + password
  ▼
LoginComponent
  │
  │  2. POST /api/auth/login  { email, password }
  ▼
AuthController
  │
  │  3. UserRepository.findByEmail(email)
  │  4. BCryptPasswordEncoder.matches(raw, hash)
  │
  ├── [fail] → 401 Unauthorized
  │
  └── [pass] → 200 { id, name, email, role, ... }
                    │
                    ▼
               AuthService stores user in localStorage (key: rm_user)
                    │
                    ▼
               Router.navigate(['/dashboard'])

Protected routes:
  Any route with canActivate: [authGuard]
        │
        ▼
  authGuard checks AuthService.isLoggedIn()
        ├── [true]  → allow navigation
        └── [false] → redirect to /login
```

---

## Database Schema

```
┌──────────────────┐       ┌──────────────────────┐
│   institutions   │       │        users          │
├──────────────────┤       ├──────────────────────┤
│ institution_id PK│       │ user_id PK            │
│ name             │       │ institution_id FK ────┼──► institutions
│ license_number   │       │ name                  │
│ email            │       │ email                 │
│ subscription_plan│       │ password_hash         │
│ created_at       │       │ role                  │
└──────────────────┘       │ mfa_enabled           │
                           │ created_at            │
                           └──────────────────────┘

┌──────────────────┐       ┌──────────────────────┐
│    borrowers     │       │        loans          │
├──────────────────┤       ├──────────────────────┤
│ borrower_id PK   │◄──────┤ borrower_id  FK       │
│ institution_id FK│       │ loan_id PK            │
│ full_name        │       │ institution_id FK     │
│ national_id      │       │ loan_amount           │
│ gender           │       │ interest_rate         │
│ age              │       │ tenure_months         │
│ location         │       │ disbursement_date     │
│ business_sector  │       │ status                │
│ monthly_income   │       │ created_at            │
│ collateral_value │       └──────────────────────┘
│ created_at       │                │
└──────────────────┘                │
                                    │
         ┌──────────────────────────┤
         │                          │
         ▼                          ▼
┌──────────────────┐       ┌──────────────────────┐
│    repayments    │       │     risk_scores       │
├──────────────────┤       ├──────────────────────┤
│ repayment_id PK  │       │ risk_score_id PK      │
│ loan_id FK       │       │ loan_id FK            │
│ amount_paid      │       │ risk_score            │
│ payment_date     │       │ risk_grade            │
│ days_past_due    │       │ probability_of_default│
│ created_at       │       │ recommended_limit     │
└──────────────────┘       │ calculation_details   │
                           │ calculated_at         │
                           └──────────────────────┘

┌──────────────────────┐   ┌──────────────────────┐
│   portfolio_metrics  │   │     audit_logs        │
├──────────────────────┤   ├──────────────────────┤
│ metrics_id PK        │   │ log_id PK             │
│ institution_id FK    │   │ user_id FK            │
│ par_30               │   │ action                │
│ par_90               │   │ entity_type           │
│ sector_concentration │   │ entity_id             │
│ region_risk_index    │   │ timestamp             │
│ forecast_default_rate│   └──────────────────────┘
│ recorded_at          │
└──────────────────────┘
```

---

## CI/CD Pipeline

```
git push / pull_request
         │
         ▼
┌────────────────────────────────────────────────────────────────────────────┐
│ GitHub Actions  (.github/workflows/ci.yml)                                 │
│                                                                            │
│  Job 1: compile-frontend          Job 2: compile-backend                  │
│  ─────────────────────────        ──────────────────────────              │
│  Node 20                          Java 21 (Temurin)                       │
│  npm ci                           ./gradlew compileJava                   │
│  ng build --configuration         ./gradlew compileTestJava               │
│           production              (no DB needed)                          │
│  artifact: frontend-dist/                                                 │
│                                                                            │
│              └──────────────────┬────────────────────┘                   │
│                                 │ both must pass                          │
│                                 ▼                                         │
│  Job 3: integration-tests                                                 │
│  ────────────────────────                                                 │
│  needs: [compile-frontend, compile-backend]                               │
│  service: postgres:15                                                     │
│  creates schema: riskmanagement_test                                      │
│  SPRING_PROFILES_ACTIVE=test                                              │
│  ./gradlew test --no-daemon                                               │
│  artifact: backend-test-results/                                          │
│  PR annotation: dorny/test-reporter                                       │
│                                                                            │
│                       │ must pass                                         │
│                       ▼                                                   │
│  Job 4: e2e-tests                                                         │
│  ────────────────                                                         │
│  needs: [integration-tests]                                               │
│  service: postgres:15                                                     │
│  creates schema: riskmanagement                                           │
│  starts: ./gradlew bootRun (background) → wait :8080                     │
│  starts: ng serve           (background) → wait :4200                    │
│  installs: Playwright Chromium                                            │
│  runs:  npx playwright test                                               │
│  artifacts: playwright-report/ · test-results/ (on failure)              │
└────────────────────────────────────────────────────────────────────────────┘

Secrets used:
  DB_PASSWORD  →  repo Settings → Secrets → Actions → DB_PASSWORD
                  (falls back to 'admin' if not set)
```

---

## Project Structure

```
riskmanagementv2/
│
├── .github/
│   └── workflows/
│       └── ci.yml                        # GitHub Actions CI/CD pipeline
│
├── .gitignore                            # Excludes: node_modules, dist, build,
│                                         #   .gradle, application-local.yml, .env
├── .vscode/
│   └── settings.json                     # Editor settings (not committed)
│
├── README.md                             # Setup guide & local run instructions
├── ARCHITECTURE.md                       # This file
├── docker-compose.yml                    # PostgreSQL 15 local dev container
│
├── backend/                              # Spring Boot application
│   ├── build.gradle                      # Java 21, Spring Boot 3.4.0, deps
│   ├── gradlew / gradlew.bat             # Gradle wrapper (no Gradle install needed)
│   ├── gradle/wrapper/
│   │   └── gradle-wrapper.properties     # Gradle version pin
│   │
│   └── src/
│       ├── main/
│       │   ├── java/com/riskmanagement/
│       │   │   │
│       │   │   ├── RiskManagementApplication.java     # @SpringBootApplication entry point
│       │   │   │
│       │   │   ├── config/
│       │   │   │   ├── CorsConfig.java                # CORS rules (allows :4200)
│       │   │   │   └── DataInitializer.java           # Seeds demo data on first start
│       │   │   │                                      # @Profile("!test") — skipped in CI
│       │   │   │
│       │   │   ├── controller/
│       │   │   │   ├── AuthController.java            # POST /api/auth/login
│       │   │   │   ├── BorrowerController.java        # CRUD /api/borrowers
│       │   │   │   ├── InstitutionController.java     # CRUD /api/institutions
│       │   │   │   ├── LoanController.java            # CRUD /api/loans
│       │   │   │   ├── LoanGraphQLController.java     # GraphQL Query + Mutation: Loan
│       │   │   │   ├── PortfolioMetricsController.java# GET/POST /api/portfolio-metrics
│       │   │   │   ├── RepaymentController.java       # GET/POST /api/repayments
│       │   │   │   ├── RiskScoreController.java       # CRUD + POST /calculate
│       │   │   │   └── UserController.java            # CRUD /api/users
│       │   │   │
│       │   │   ├── model/
│       │   │   │   ├── AuditLog.java                  # @Entity — audit trail
│       │   │   │   ├── Borrower.java                  # @Entity — borrower profile
│       │   │   │   ├── Institution.java               # @Entity — MFI institution
│       │   │   │   ├── Loan.java                      # @Entity — loan record
│       │   │   │   ├── PortfolioMetrics.java          # @Entity — PAR30/PAR90/etc.
│       │   │   │   ├── Repayment.java                 # @Entity — repayment installment
│       │   │   │   ├── RiskScore.java                 # @Entity — calculated risk result
│       │   │   │   └── User.java                      # @Entity — platform user
│       │   │   │
│       │   │   ├── repository/
│       │   │   │   ├── AuditLogRepository.java        # JpaRepository<AuditLog, Long>
│       │   │   │   ├── BorrowerRepository.java        # JpaRepository<Borrower, Long>
│       │   │   │   ├── InstitutionRepository.java     # JpaRepository<Institution, Long>
│       │   │   │   ├── LoanRepository.java            # JpaRepository<Loan, Long>
│       │   │   │   ├── PortfolioMetricsRepository.java# JpaRepository<PortfolioMetrics, Long>
│       │   │   │   ├── RepaymentRepository.java       # + findByLoanId(Long)
│       │   │   │   ├── RiskScoreRepository.java       # JpaRepository<RiskScore, Long>
│       │   │   │   └── UserRepository.java            # + findByEmail(String)
│       │   │   │
│       │   │   └── service/
│       │   │       ├── AuditLogService.java           # CRUD for audit logs
│       │   │       ├── BorrowerService.java           # CRUD for borrowers
│       │   │       ├── InstitutionService.java        # CRUD for institutions
│       │   │       ├── LoanService.java               # CRUD for loans
│       │   │       ├── PortfolioMetricsService.java   # CRUD for metrics
│       │   │       ├── RepaymentService.java          # CRUD for repayments
│       │   │       ├── RiskCalculationService.java    # Core scoring engine
│       │   │       │                                  #   income(25%) + repayment(30%)
│       │   │       │                                  #   + collateral(20%) + sector(15%)
│       │   │       │                                  #   + location(10%)
│       │   │       ├── RiskScoreService.java          # CRUD for persisted risk scores
│       │   │       └── UserService.java               # CRUD for users
│       │   │
│       │   └── resources/
│       │       ├── application.yml                    # Main config (env var placeholders)
│       │       ├── application-local.yml              # Local overrides — NOT committed
│       │       └── graphql/
│       │           └── schema.graphqls                # GraphQL schema (Loan CRUD)
│       │
│       └── test/
│           ├── java/com/riskmanagement/integration/
│           │   ├── AuditLogIntegrationTest.java
│           │   ├── BorrowerIntegrationTest.java
│           │   ├── CompleteFlowIntegrationTest.java   # End-to-end domain flow
│           │   ├── InstitutionIntegrationTest.java
│           │   ├── LoanIntegrationTest.java
│           │   ├── PortfolioMetricsIntegrationTest.java
│           │   ├── RepaymentIntegrationTest.java
│           │   ├── RiskCalculationIntegrationTest.java
│           │   └── UserIntegrationTest.java
│           │
│           └── resources/
│               └── application-test.yml              # Test profile: riskmanagement_test
│                                                     # schema, create-drop, no GraphiQL
│
└── frontend/                             # Angular 17 SPA
    ├── angular.json                      # Angular CLI workspace config
    ├── package.json                      # Dependencies + npm scripts
    ├── package-lock.json                 # Locked dependency tree (commit this)
    ├── tsconfig.json                     # Base TS config (target ES2022, strict:false)
    ├── tsconfig.app.json                 # App build TS config
    ├── tsconfig.spec.json                # Karma/Jasmine unit test TS config
    ├── tsconfig.e2e.json                 # Playwright TS config (module: CommonJS)
    ├── playwright.config.ts              # Playwright config (baseURL :4200, Chromium)
    │
    ├── e2e/                              # Playwright E2E tests
    │   ├── helpers/
    │   │   └── auth.helper.ts            # loginViaUI(), loginViaStorage(), logout()
    │   ├── auth.spec.ts                  # 6 tests — login / guard / logout
    │   ├── dashboard.spec.ts             # 6 tests — KPIs, charts, navigation
    │   ├── loan-flow.spec.ts             # 8 tests — form → risk result flow
    │   └── navigation.spec.ts            # 8 tests — routes, sidebar, header
    │
    └── src/
        ├── index.html                    # App shell
        ├── main.ts                       # Bootstrap (standalone)
        ├── styles.scss                   # Global styles
        │
        └── app/
            ├── app.component.ts/html/scss # Root component (router-outlet)
            ├── app.config.ts             # provideRouter, provideHttpClient, etc.
            ├── app.routes.ts             # Route definitions (all lazy-loaded)
            │
            ├── core/
            │   ├── guards/
            │   │   └── auth.guard.ts     # CanActivateFn — checks localStorage
            │   └── services/
            │       ├── auth.service.ts   # login(), logout(), isLoggedIn()
            │       │                     # API: POST /api/auth/login
            │       ├── loan.service.ts   # getLoans(), calculateRiskScore()
            │       │                     # API: /api/loans, /api/risk-scores/calculate
            │       └── portfolio.service.ts # getDashboardData(), getPortfolioData()
            │                               # API: forkJoin of loans/borrowers/scores/metrics
            │
            ├── screens/
            │   ├── login/                # /login — email + password form
            │   ├── dashboard/            # /dashboard — KPIs + charts (authGuard)
            │   ├── new-loan/             # /new-loan — loan application form (authGuard)
            │   ├── risk-result/          # /risk-result — score display (authGuard)
            │   ├── portfolio-analytics/  # /portfolio — analytics charts (authGuard)
            │   └── report-generator/     # /reports — PDF export (authGuard)
            │
            └── shared/
                ├── header/               # app-header — top navigation bar
                └── sidebar/              # app-sidebar — left navigation menu
```

---

## Backend Layers

```
Request
   │
   ▼
┌──────────────────────────────────────┐
│         Controller Layer             │  @RestController / @Controller
│  Validates HTTP input, maps to DTOs  │  No business logic
└──────────────────────┬───────────────┘
                       │
                       ▼
┌──────────────────────────────────────┐
│          Service Layer               │  @Service
│  Orchestrates business logic         │  Calls one or more repositories
│  RiskCalculationService is the       │  Stateless
│  core engine (weighted scoring)      │
└──────────────────────┬───────────────┘
                       │
                       ▼
┌──────────────────────────────────────┐
│        Repository Layer              │  @Repository (Spring Data JPA)
│  Database access via derived queries │  JpaRepository<Entity, Long>
│  or JPQL                             │  Managed by Hibernate
└──────────────────────┬───────────────┘
                       │
                       ▼
┌──────────────────────────────────────┐
│        Model / Entity Layer          │  @Entity
│  JPA-annotated POJOs                 │  Maps to PostgreSQL tables
│  schema: riskmanagement              │
└──────────────────────────────────────┘
```

---

## Frontend Layers

```
┌─────────────────────────────────────────────────────┐
│  Pages (Screens)                                    │
│  login · dashboard · new-loan · risk-result         │
│  portfolio-analytics · report-generator             │
│  Each is a standalone Angular component             │
└───────────────────────┬─────────────────────────────┘
                        │ calls
                        ▼
┌─────────────────────────────────────────────────────┐
│  Services (Core)                                    │
│  AuthService       — session management             │
│  LoanService       — loan + risk score API calls    │
│  PortfolioService  — aggregated dashboard data      │
└───────────────────────┬─────────────────────────────┘
                        │ HttpClient
                        ▼
┌─────────────────────────────────────────────────────┐
│  Spring Boot REST API  (:8080)                      │
└─────────────────────────────────────────────────────┘

Route protection:
  All screens except /login are wrapped with authGuard.
  authGuard reads localStorage key rm_user set by AuthService.
```

---

## API Reference

### REST Endpoints

| Method | Path                          | Description                          |
|--------|-------------------------------|--------------------------------------|
| POST   | /api/auth/login               | Authenticate user, returns user object |
| GET    | /api/loans                    | List all loans                       |
| GET    | /api/loans/{id}               | Get loan by ID                       |
| POST   | /api/loans                    | Create loan                          |
| PUT    | /api/loans/{id}               | Update loan                          |
| DELETE | /api/loans/{id}               | Delete loan                          |
| GET    | /api/borrowers                | List all borrowers                   |
| GET    | /api/borrowers/{id}           | Get borrower by ID                   |
| POST   | /api/borrowers                | Create borrower                      |
| PUT    | /api/borrowers/{id}           | Update borrower                      |
| DELETE | /api/borrowers/{id}           | Delete borrower                      |
| GET    | /api/users                    | List all users                       |
| POST   | /api/users                    | Create user                          |
| PUT    | /api/users/{id}               | Update user                          |
| DELETE | /api/users/{id}               | Delete user                          |
| GET    | /api/risk-scores              | List all risk scores                 |
| POST   | /api/risk-scores/calculate    | Calculate risk score (no persist)    |
| POST   | /api/risk-scores              | Persist a risk score                 |
| GET    | /api/repayments               | List repayments                      |
| POST   | /api/repayments               | Add repayment                        |
| GET    | /api/portfolio-metrics        | Get portfolio metrics                |
| GET    | /api/institutions             | List institutions                    |

### GraphQL (POST /graphql)

```graphql
# Queries
loans: [Loan]
loan(id: ID!): Loan

# Mutations
createLoan(loan: LoanInput!): Loan
updateLoan(id: ID!, loan: LoanInput!): Loan
deleteLoan(id: ID!): Boolean
```

Interactive playground: http://localhost:8080/graphiql
