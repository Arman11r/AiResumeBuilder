package com.resumeai.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendNotificationRequest {
    @NotBlank
    private String recipientId;
    @NotBlank
    private String type;
    @NotBlank
    private String title;
    @NotBlank
    private String message;
    private String channel = "APP";
    private String relatedId;
    private String relatedType;
}