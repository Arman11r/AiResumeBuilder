package com.resumeai.jobmatch.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class JobMatchResponse {
    private String matchId;
    private String resumeId;
    private String userId;
    private String jobTitle;
    private String companyName;
    private String applyUrl;
    private String jobDescription;
    private int matchScore;
    private String missingSkills;
    private String recommendations;
    private String source;
    private boolean isBookmarked;
    private LocalDateTime matchedAt;
}