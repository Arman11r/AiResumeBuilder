CREATE TABLE export_jobs (
                             job_id VARCHAR(36) NOT NULL PRIMARY KEY,
                             resume_id VARCHAR(36) NOT NULL,
                             user_id VARCHAR(36) NOT NULL,
                             format ENUM('PDF', 'DOCX', 'JSON') NOT NULL,
                             status ENUM('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED', 'EXPIRED') NOT NULL DEFAULT 'QUEUED',
                             file_url VARCHAR(500),
                             file_size_kb INT,
                             template_id VARCHAR(36),
                             customizations TEXT,
                             requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             completed_at TIMESTAMP NULL,
                             expires_at TIMESTAMP NULL
);

CREATE INDEX idx_export_jobs_user ON export_jobs(user_id);
CREATE INDEX idx_export_jobs_resume ON export_jobs(resume_id);
CREATE INDEX idx_export_jobs_status ON export_jobs(status);