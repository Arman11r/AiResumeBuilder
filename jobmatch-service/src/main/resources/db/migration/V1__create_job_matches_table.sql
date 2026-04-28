CREATE TABLE job_matches (
                             match_id VARCHAR(36) NOT NULL PRIMARY KEY,
                             resume_id VARCHAR(36) NOT NULL,
                             user_id VARCHAR(36) NOT NULL,
                             job_title VARCHAR(200) NOT NULL,
                             job_description TEXT,
                             match_score INT DEFAULT 0,
                             missing_skills TEXT,
                             recommendations TEXT,
                             source ENUM('LINKEDIN', 'NAUKRI', 'MANUAL') NOT NULL DEFAULT 'MANUAL',
                             matched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             is_bookmarked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_job_matches_user ON job_matches(user_id);
CREATE INDEX idx_job_matches_resume ON job_matches(resume_id);
CREATE INDEX idx_job_matches_score ON job_matches(match_score DESC);