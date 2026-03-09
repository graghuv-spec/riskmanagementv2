-- This script runs automatically on first container start
-- (postgres Docker image executes all *.sql files in /docker-entrypoint-initdb.d/)

CREATE SCHEMA IF NOT EXISTS riskmanagement;
