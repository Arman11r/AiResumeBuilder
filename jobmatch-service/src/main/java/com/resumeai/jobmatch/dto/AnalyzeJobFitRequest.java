package com.resumeai.jobmatch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnalyzeJobFitRequest {
    @NotBlank
    private String resumeId;
    @NotBlank
    private String userId;
    @NotBlank
    private String jobTitle;
    @NotBlank
    private String jobDescription;
}