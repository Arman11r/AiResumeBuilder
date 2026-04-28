package com.resumeai.ai.service;

import com.resumeai.ai.dto.*;
import java.util.List;

public interface AiService {
    AiResponse generateSummary(AiRequestDTO request);
    AiResponse generateBullets(AiRequestDTO request);
    AiResponse generateCoverLetter(AiRequestDTO request);
    AiResponse improveSection(AiRequestDTO request);
    AtsResult checkAtsCompatibility(AiRequestDTO request);
    AiResponse suggestSkills(AiRequestDTO request);
    AiResponse translateResume(AiRequestDTO request);
    AiResponse tailorResume(AiRequestDTO request);
    List<AiResponse> getHistory(String userId);
    QuotaResponse getQuota(String userId);
}