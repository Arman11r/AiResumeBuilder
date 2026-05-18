-- Increase apply_url length to accommodate very long tracking URLs from job boards like Naukri
ALTER TABLE job_matches MODIFY COLUMN apply_url TEXT;
