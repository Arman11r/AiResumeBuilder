package com.resumeai.section.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSectionRequest {

    @NotBlank
    private String resumeId;

    @NotNull
    private String sectionType;

    @NotBlank
    private String title;

    private String content;
    private int displayOrder = 0;
    private boolean visible = true;
    private boolean aiGenerated = false;
}