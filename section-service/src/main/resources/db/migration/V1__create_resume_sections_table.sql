CREATE TABLE resume_sections (
                                 section_id    VARCHAR(36)   NOT NULL PRIMARY KEY,
                                 resume_id     VARCHAR(36)   NOT NULL,
                                 section_type  ENUM(
                                     'SUMMARY','EXPERIENCE','EDUCATION','SKILLS',
                                     'CERTIFICATIONS','PROJECTS','LANGUAGES','VOLUNTEER','CUSTOM'
                                     ) NOT NULL,
                                 title         VARCHAR(200)  NOT NULL,
                                 content       TEXT,
                                 display_order INT           NOT NULL DEFAULT 0,
                                 is_visible    BOOLEAN       NOT NULL DEFAULT TRUE,
                                 ai_generated  BOOLEAN       NOT NULL DEFAULT FALSE,
                                 created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 INDEX idx_resume_id (resume_id),
                                 INDEX idx_resume_type (resume_id, section_type)
);