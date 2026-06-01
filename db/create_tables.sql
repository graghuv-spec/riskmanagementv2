-- SQL script to create required tables for riskmanagement backend

CREATE SCHEMA IF NOT EXISTS riskmanagement;

-- Users table
CREATE TABLE IF NOT EXISTS riskmanagement.users (
    user_id SERIAL PRIMARY KEY,
    institution_id BIGINT,
    name VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    role VARCHAR(50),
    password_hash VARCHAR(255),
    mfa_enabled BOOLEAN,
    created_at TIMESTAMP,
    last_login TIMESTAMP
);

-- Borrowers table
CREATE TABLE IF NOT EXISTS riskmanagement.borrowers (
    borrower_id SERIAL PRIMARY KEY,
    user_id BIGINT,
    institution_id BIGINT,
    full_name VARCHAR(255),
    national_id VARCHAR(100),
    gender VARCHAR(20),
    age INT,
    location VARCHAR(255),
    business_sector VARCHAR(255),
    monthly_income DOUBLE PRECISION,
    collateral_value DOUBLE PRECISION,
    credit_score INT,
    created_at TIMESTAMP
);

-- Loans table
CREATE TABLE IF NOT EXISTS riskmanagement.loans (
    loan_id SERIAL PRIMARY KEY,
    borrower_id BIGINT,
    institution_id BIGINT,
    loan_amount DOUBLE PRECISION,
    interest_rate DOUBLE PRECISION,
    tenure_months INT,
    disbursement_date TIMESTAMP,
    status VARCHAR(50),
    created_at TIMESTAMP
);
