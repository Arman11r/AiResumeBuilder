package com.resumeai.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResumeResponse {
    private String resumeId;
    private String userId;
    private String title;
    private String targetJobTitle;
    private String templateId;
    private Integer atsScore;
    private String status;
    private String language;
    private boolean isPublic;
    private int viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}