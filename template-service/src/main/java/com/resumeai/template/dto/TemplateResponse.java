package com.resumeai.template.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("isPremium")
    private boolean isPremium;
    @JsonProperty("isActive")
    private boolean isActive;
    private int usageCount;
    private LocalDateTime createdAt;
}