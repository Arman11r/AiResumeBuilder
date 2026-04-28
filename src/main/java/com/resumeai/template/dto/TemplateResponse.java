package com.resumeai.template.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class TemplateResponse {
    private String templateId;
    private String name;
    private String description;
    private String thumbnailUrl;
    private String category;
    private boolean isPremium;
    private boolean isActive;
    private int usageCount;
    private LocalDateTime createdAt;
}