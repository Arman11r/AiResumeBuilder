package com.resumeai.template.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTemplateRequest {
    @NotBlank
    private String name;
    private String description;
    private String thumbnailUrl;
    private String htmlLayout;
    private String cssStyles;
    @NotBlank
    private String category;
    private boolean isPremium;
}