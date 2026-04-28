package com.resumeai.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiRequestDTO {
    @NotBlank
    private String userId;
    private String resumeId;
    private String jobTitle;
    private int yearsOfExperience;
    private String skills;
    private String jobDescription;
    private String sectionContent;
    private String targetLanguage;
    private String improveGoal;
}