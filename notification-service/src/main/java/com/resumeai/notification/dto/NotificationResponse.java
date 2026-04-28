package com.resumeai.notification.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class NotificationResponse {
    private String notificationId;
    private String recipientId;
    private String type;
    private String title;
    private String message;
    private String channel;
    private String relatedId;
    private String relatedType;
    private boolean isRead;
    private LocalDateTime sentAt;
}