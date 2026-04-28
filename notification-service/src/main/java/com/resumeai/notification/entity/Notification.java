package com.resumeai.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "notification_id", updatable = false, nullable = false)
    private String notificationId;

    @Column(name = "recipient_id", nullable = false)
    private String recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    private Channel channel = Channel.APP;

    @Column(name = "related_id")
    private String relatedId;

    @Column(name = "related_type")
    private String relatedType;

    @Column(name = "is_read")
    private boolean isRead = false;

    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        this.sentAt = LocalDateTime.now();
    }

    public enum NotificationType {
        ATS_COMPLETE, EXPORT_READY, AI_DONE,
        JOB_MATCH, PLAN_CHANGE, QUOTA_WARNING
    }

    public enum Channel {
        APP, EMAIL
    }
}