package com.resumeai.template.service;

import com.resumeai.template.dto.CreateTemplateRequest;
import com.resumeai.template.dto.TemplateResponse;
import java.util.List;

public interface TemplateService {
    TemplateResponse createTemplate(CreateTemplateRequest request);
    TemplateResponse getById(String templateId);
    List<TemplateResponse> getAllTemplates();
    List<TemplateResponse> getFreeTemplates();
    List<TemplateResponse> getPremiumTemplates();
    List<TemplateResponse> getByCategory(String category);
    List<TemplateResponse> getPopularTemplates();
    TemplateResponse updateTemplate(String templateId, CreateTemplateRequest request);
    void deactivateTemplate(String templateId);
    void incrementUsage(String templateId);
}