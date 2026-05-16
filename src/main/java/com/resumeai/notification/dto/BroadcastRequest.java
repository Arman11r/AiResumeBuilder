package com.resumeai.notification.dto;

import lombok.Data;

@Data
public class BroadcastRequest {
    private String title;
    private String message;
    private String type = "ADMIN_BROADCAST";
    private java.util.List<String> recipientIds;  // if null/empty → the notification controller should prefill
    private String targetTier;  // ALL, FREE, or PREMIUM (informational, used by the gateway/admin)
}