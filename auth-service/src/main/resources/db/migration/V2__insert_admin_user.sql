-- Insert hardcoded admin user if it doesn't exist
INSERT INTO users (user_id, full_name, email, password_hash, phone, role, provider, is_active, subscription_plan)
VALUES ('admin-id-1234', 'System Admin', 'admin@resumeai.com', '$2a$12$4Q7KDzkMP4acT9G0SEvDMutQpbUhD9OASDSFzZhqzofYWSJKYRDsq', '1234567890', 'ADMIN', 'LOCAL', TRUE, 'PREMIUM')
ON DUPLICATE KEY UPDATE role='ADMIN';
