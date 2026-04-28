package com.resumeai.resume.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateResumeRequest {

    @NotBlank
    private String title;

    private String targetJobTitle;
    private String templateId;
    private String language = "en";
}