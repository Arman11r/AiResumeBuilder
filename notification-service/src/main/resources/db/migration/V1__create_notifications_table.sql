CREATE TABLE notifications (
                               notification_id VARCHAR(36) NOT NULL PRIMARY KEY,
                               recipient_id VARCHAR(36) NOT NULL,
                               type ENUM(
                                   'ATS_COMPLETE',
                                   'EXPORT_READY',
                                   'AI_DONE',
                                   'JOB_MATCH',
                                   'PLAN_CHANGE',
                                   'QUOTA_WARNING'
                                   ) NOT NULL,
                               title VARCHAR(200) NOT NULL,
                               message TEXT NOT NULL,
                               channel ENUM('APP', 'EMAIL') NOT NULL DEFAULT 'APP',
                               related_id VARCHAR(36),
                               related_type VARCHAR(50),
                               is_read BOOLEAN NOT NULL DEFAULT FALSE,
                               sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_id);
CREATE INDEX idx_notifications_unread ON notifications(recipient_id, is_read);