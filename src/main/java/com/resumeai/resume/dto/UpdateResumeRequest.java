package com.resumeai.resume.dto;

import lombok.Data;

@Data
public class UpdateResumeRequest {
    private String title;
    private String targetJobTitle;
    private String templateId;
    private String language;
    private String status;
}