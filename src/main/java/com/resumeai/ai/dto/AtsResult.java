package com.resumeai.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class AtsResult {
    private int score;
    private String missingKeywords;
    private String presentKeywords;
    private String suggestions;
    private String requestId;
}