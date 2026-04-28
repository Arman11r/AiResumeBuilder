-- Add company_name and apply_url columns for live job listings
ALTER TABLE job_matches ADD COLUMN company_name VARCHAR(200) NULL;
ALTER TABLE job_matches ADD COLUMN apply_url    VARCHAR(1000) NULL;
