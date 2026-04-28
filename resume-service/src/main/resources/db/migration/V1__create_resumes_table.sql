CREATE TABLE resumes (
                         resume_id    VARCHAR(36)  NOT NULL PRIMARY KEY,
                         user_id      VARCHAR(36)  NOT NULL,
                         title        VARCHAR(200) NOT NULL,
                         target_job_title VARCHAR(200),
                         template_id  VARCHAR(36),
                         ats_score    INT          DEFAULT NULL,
                         status       ENUM('DRAFT','COMPLETE') NOT NULL DEFAULT 'DRAFT',
                         language     VARCHAR(10)  NOT NULL DEFAULT 'en',
                         is_public    BOOLEAN      NOT NULL DEFAULT FALSE,
                         view_count   INT          NOT NULL DEFAULT 0,
                         created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         INDEX idx_user_id (user_id),
                         INDEX idx_is_public (is_public)
);