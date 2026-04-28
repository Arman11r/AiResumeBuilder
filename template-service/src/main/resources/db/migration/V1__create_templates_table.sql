CREATE TABLE resume_templates (
                                  template_id VARCHAR(36) NOT NULL PRIMARY KEY,
                                  name VARCHAR(100) NOT NULL,
                                  description VARCHAR(500),
                                  thumbnail_url VARCHAR(500),
                                  html_layout LONGTEXT,
                                  css_styles LONGTEXT,
                                  category ENUM('PROFESSIONAL','CREATIVE','MODERN','MINIMALIST','ATS_OPTIMISED') NOT NULL DEFAULT 'PROFESSIONAL',
                                  is_premium BOOLEAN NOT NULL DEFAULT FALSE,
                                  is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                  usage_count INT NOT NULL DEFAULT 0,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO resume_templates (template_id, name, description, thumbnail_url, category, is_premium, is_active, usage_count) VALUES
                                                                                                                               ('tmpl-001', 'Classic Professional', 'Clean single-column layout ideal for corporate roles', '/thumbnails/classic.png', 'PROFESSIONAL', FALSE, TRUE, 0),
                                                                                                                               ('tmpl-002', 'Modern Minimal', 'Two-column minimalist design with accent colors', '/thumbnails/modern.png', 'MINIMALIST', FALSE, TRUE, 0),
                                                                                                                               ('tmpl-003', 'ATS Optimised', 'Plain text-friendly layout for ATS systems', '/thumbnails/ats.png', 'ATS_OPTIMISED', FALSE, TRUE, 0),
                                                                                                                               ('tmpl-004', 'Creative Portfolio', 'Bold creative layout for designers and artists', '/thumbnails/creative.png', 'CREATIVE', TRUE, TRUE, 0),
                                                                                                                               ('tmpl-005', 'Executive Modern', 'Premium executive layout with sidebar', '/thumbnails/executive.png', 'MODERN', TRUE, TRUE, 0);