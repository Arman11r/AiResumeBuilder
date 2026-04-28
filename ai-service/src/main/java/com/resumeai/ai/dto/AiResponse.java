package com.resumeai.ai.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class AiResponse {
    private String requestId;
    private String userId;
    private String requestType;
    private String content;
    private String model;
    private int tokensUsed;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}