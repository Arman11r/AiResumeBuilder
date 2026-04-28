CREATE TABLE users (
                       user_id VARCHAR(36) NOT NULL PRIMARY KEY,
                       full_name VARCHAR(100) NOT NULL,
                       email VARCHAR(150) NOT NULL UNIQUE,
                       password_hash VARCHAR(255),
                       phone VARCHAR(20),
                       role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
                       provider ENUM('LOCAL', 'GOOGLE', 'LINKEDIN') NOT NULL DEFAULT 'LOCAL',
                       is_active BOOLEAN NOT NULL DEFAULT TRUE,
                       subscription_plan ENUM('FREE', 'PREMIUM') NOT NULL DEFAULT 'FREE',
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);