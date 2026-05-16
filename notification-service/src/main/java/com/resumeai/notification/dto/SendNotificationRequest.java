package com.resumeai.notification.dto;

import lombok.Data;

@Data
public class SendNotificationRequest {
    private String recipientId;
    private String type;
    private String title;   // optional — defaults to type label if blank
    private String message;
    private String channel = "APP";
    private String relatedId;
    private String relatedType;
}