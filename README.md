# Risk Management Project v2

A comprehensive risk management system for financial institutions, built with Java Spring Boot backend, PostgreSQL database, GraphQL API, and Angular frontend.

## Features

- User management with role-based access
- Borrower and loan management
- Risk scoring with AI models (rule-based, statistical, ML-ready)
- Portfolio analytics
- Audit logging
- GraphQL API for frontend integration

## Architecture

- **Backend**: Java 17, Spring Boot 3, Gradle
- **Database**: PostgreSQL (Docker)
- **API**: REST and GraphQL
- **Frontend**: Angular 17

## Setup Instructions

### Prerequisites

- Java 17
- Gradle
- Node.js 18+
- Angular CLI
- Docker Desktop

### Database Setup

1. Start PostgreSQL:
   ```bash
   docker-compose up -d
   ```

### Backend Setup

1. Navigate to backend directory:
   ```bash
   cd backend
   ```

2. Build and run:
   ```bash
   ./gradlew bootRun
   ```

Backend will be available at http://localhost:8080

GraphQL playground at http://localhost:8080/graphiql

### Frontend Setup

1. Install dependencies:
   ```bash
   cd frontend
   npm install
   ```

2. Start development server:
   ```bash
   ng serve
   ```

Frontend will be available at http://localhost:4200

## API Endpoints

### REST API

- Users: `/api/users`
- Borrowers: `/api/borrowers`
- Loans: `/api/loans`
- Risk Scores: `/api/risk-scores`

### GraphQL

- Endpoint: `/graphql`
- Schema available at `/graphiql`

## Database Schema

See the model classes for the complete schema with relationships.

## Risk Calculation

Implements rule-based risk scoring with weights for income, repayment history, collateral, sector, and location.

Future phases include statistical models and machine learning.
