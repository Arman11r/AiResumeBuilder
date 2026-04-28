package com.resumeai.template.service.impl;

import com.resumeai.template.dto.CreateTemplateRequest;
import com.resumeai.template.dto.TemplateResponse;
import com.resumeai.template.entity.ResumeTemplate;
import com.resumeai.template.repository.TemplateRepository;
import com.resumeai.template.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;

    @Override
    @Transactional
    public TemplateResponse createTemplate(CreateTemplateRequest request) {
        ResumeTemplate template = ResumeTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .thumbnailUrl(request.getThumbnailUrl())
                .htmlLayout(request.getHtmlLayout())
                .cssStyles(request.getCssStyles())
                .category(parseCategory(request.getCategory()))
                .isPremium(request.isPremium())
                .isActive(true)
                .usageCount(0)
                .build();
        return toResponse(templateRepository.save(template));
    }

    @Override
    public TemplateResponse getById(String templateId) {
        return toResponse(findOrThrow(templateId));
    }

    @Override
    public List<TemplateResponse> getAllTemplates() {
        return templateRepository.findByIsActiveTrue()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<TemplateResponse> getFreeTemplates() {
        return templateRepository.findByIsPremiumFalseAndIsActiveTrue()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<TemplateResponse> getPremiumTemplates() {
        return templateRepository.findByIsPremiumTrueAndIsActiveTrue()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<TemplateResponse> getByCategory(String category) {
        return templateRepository.findByCategoryAndIsActiveTrue(parseCategory(category))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<TemplateResponse> getPopularTemplates() {
        return templateRepository.findAllByOrderByUsageCountDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TemplateResponse updateTemplate(String templateId, CreateTemplateRequest request) {
        ResumeTemplate template = findOrThrow(templateId);
        if (request.getName() != null)        template.setName(request.getName());
        if (request.getDescription() != null) template.setDescription(request.getDescription());
        if (request.getHtmlLayout() != null)  template.setHtmlLayout(request.getHtmlLayout());
        if (request.getCssStyles() != null)   template.setCssStyles(request.getCssStyles());
        if (request.getCategory() != null)    template.setCategory(parseCategory(request.getCategory()));
        template.setPremium(request.isPremium());
        return toResponse(templateRepository.save(template));
    }

    @Override
    @Transactional
    public void deactivateTemplate(String templateId) {
        ResumeTemplate template = findOrThrow(templateId);
        template.setActive(false);
        templateRepository.save(template);
    }

    @Override
    @Transactional
    public void incrementUsage(String templateId) {
        ResumeTemplate template = findOrThrow(templateId);
        template.setUsageCount(template.getUsageCount() + 1);
        templateRepository.save(template);
    }

    private ResumeTemplate findOrThrow(String templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Template not found: " + templateId));
    }

    private ResumeTemplate.Category parseCategory(String category) {
        try {
            return ResumeTemplate.Category.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid category: " + category);
        }
    }

    private TemplateResponse toResponse(ResumeTemplate t) {
        return TemplateResponse.builder()
                .templateId(t.getTemplateId())
                .name(t.getName())
                .description(t.getDescription())
                .thumbnailUrl(t.getThumbnailUrl())
                .category(t.getCategory().name())
                .isPremium(t.isPremium())
                .isActive(t.isActive())
                .usageCount(t.getUsageCount())
                .createdAt(t.getCreatedAt())
                .build();
    }
}