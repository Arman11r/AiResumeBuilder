package com.resumeai.section.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SectionResponse {
    private String sectionId;
    private String resumeId;
    private String sectionType;
    private String title;
    private String content;
    private int displayOrder;
    private boolean visible;
    private boolean aiGenerated;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}