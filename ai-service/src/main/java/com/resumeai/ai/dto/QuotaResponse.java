package com.resumeai.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class QuotaResponse {
    private String userId;
    private long contentCallsUsed;
    private long contentCallsRemaining;
    private long atsCallsUsed;
    private long atsCallsRemaining;
    private boolean isPremium;
}