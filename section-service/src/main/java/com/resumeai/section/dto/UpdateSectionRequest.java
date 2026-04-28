package com.resumeai.section.dto;

import lombok.Data;

@Data
public class UpdateSectionRequest {
    private String title;
    private String content;
    private Integer displayOrder;
    private Boolean visible;
    private Boolean aiGenerated;
}