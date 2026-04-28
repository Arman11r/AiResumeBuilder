package com.resumeai.export.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExportRequest {
    @NotBlank
    private String resumeId;
    @NotBlank
    private String userId;
    private String templateId;
    private String customizations;
}