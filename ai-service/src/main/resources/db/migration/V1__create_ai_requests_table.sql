CREATE TABLE ai_requests (
                             request_id VARCHAR(36) NOT NULL PRIMARY KEY,
                             user_id VARCHAR(36) NOT NULL,
                             resume_id VARCHAR(36),
                             request_type ENUM(
                                 'SUMMARY',
                                 'BULLETS',
                                 'COVER_LETTER',
                                 'IMPROVE',
                                 'ATS',
                                 'SKILLS',
                                 'TAILOR',
                                 'TRANSLATE'
                                 ) NOT NULL,
                             input_prompt TEXT NOT NULL,
                             ai_response LONGTEXT,
                             model ENUM('GPT4O', 'CLAUDE', 'GEMINI') NOT NULL DEFAULT 'GPT4O',
                             tokens_used INT DEFAULT 0,
                             status ENUM('QUEUED', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'QUEUED',
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             completed_at TIMESTAMP NULL
);

CREATE INDEX idx_ai_requests_user ON ai_requests(user_id);
CREATE INDEX idx_ai_requests_type ON ai_requests(request_type);
CREATE INDEX idx_ai_requests_status ON ai_requests(status);