package com.resumeai.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @UuidGenerator
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
        // AI events
        ATS_COMPLETE, AI_DONE, AI_CONTENT_GENERATED, COVER_LETTER_GENERATED,
        // Resume events
        RESUME_CREATED, RESUME_DELETED, RESUME_PUBLISHED, RESUME_UNPUBLISHED,
        SECTION_ADDED, SECTION_DELETED,
        // Export events
        EXPORT_READY,
        // Job events
        JOB_MATCH, JOB_SEARCH_COMPLETE, JOB_BOOKMARKED,
        // Account events
        PLAN_UPGRADED, PLAN_CHANGE, QUOTA_WARNING, PROFILE_UPDATED, PASSWORD_CHANGED,
        // Admin events
        ADMIN_BROADCAST
    }

    public enum Channel {
        APP, EMAIL
    }
}