-- ResumeAI — Create all service databases on first boot
-- This script runs once when the MySQL container initialises from a fresh volume.

CREATE DATABASE IF NOT EXISTS `resumeai_auth`      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `resumeai_resume`    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `resumeai_section`   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `resumeai_ai`        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `resumeai_template`  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `resumeai_export`    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `resumeai_jobmatch`  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `resumeai_notification` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant root full access (already implicit for root@localhost, explicit for root@%)
GRANT ALL PRIVILEGES ON `resumeai_auth`.*         TO 'root'@'%';
GRANT ALL PRIVILEGES ON `resumeai_resume`.*       TO 'root'@'%';
GRANT ALL PRIVILEGES ON `resumeai_section`.*      TO 'root'@'%';
GRANT ALL PRIVILEGES ON `resumeai_ai`.*           TO 'root'@'%';
GRANT ALL PRIVILEGES ON `resumeai_template`.*     TO 'root'@'%';
GRANT ALL PRIVILEGES ON `resumeai_export`.*       TO 'root'@'%';
GRANT ALL PRIVILEGES ON `resumeai_jobmatch`.*     TO 'root'@'%';
GRANT ALL PRIVILEGES ON `resumeai_notification`.* TO 'root'@'%';
FLUSH PRIVILEGES;
