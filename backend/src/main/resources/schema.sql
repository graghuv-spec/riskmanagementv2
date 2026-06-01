
CREATE SCHEMA IF NOT EXISTS riskmanagement;

-- Add credit_score column to borrowers table
ALTER TABLE IF EXISTS riskmanagement.borrowers
    ADD COLUMN IF NOT EXISTS credit_score INTEGER;

ALTER TABLE IF EXISTS public.borrowers
    ADD COLUMN IF NOT EXISTS credit_score INTEGER;
