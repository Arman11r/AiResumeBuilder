package com.resumeai.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BroadcastRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String message;
    private String type = "PLAN_CHANGE";
    private java.util.List<String> recipientIds;
}