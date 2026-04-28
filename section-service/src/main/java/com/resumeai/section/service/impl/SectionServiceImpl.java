package com.resumeai.section.service.impl;

import com.resumeai.section.dto.*;
import com.resumeai.section.entity.ResumeSection;
import com.resumeai.section.repository.ResumeSectionRepository;
import com.resumeai.section.service.SectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SectionServiceImpl implements SectionService {

    private final ResumeSectionRepository sectionRepository;

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SectionResponse createSection(CreateSectionRequest request) {
        ResumeSection section = ResumeSection.builder()
                .resumeId(request.getResumeId())
                .sectionType(parseSectionType(request.getSectionType()))
                .title(request.getTitle())
                .content(request.getContent())
                .displayOrder(request.getDisplayOrder())
                .isVisible(request.isVisible())
                .aiGenerated(request.isAiGenerated())
                .build();
        return toResponse(sectionRepository.save(section));
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    public SectionResponse getSectionById(String sectionId) {
        return toResponse(findOrThrow(sectionId));
    }

    @Override
    public List<SectionResponse> getSectionsByResumeId(String resumeId) {
        return sectionRepository.findByResumeIdOrderByDisplayOrderAsc(resumeId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<SectionResponse> getSectionsByType(String resumeId, String sectionType) {
        return sectionRepository
                .findByResumeIdAndSectionTypeOrderByDisplayOrderAsc(resumeId, parseSectionType(sectionType))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SectionResponse updateSection(String sectionId, UpdateSectionRequest request) {
        ResumeSection section = findOrThrow(sectionId);
        if (request.getTitle() != null)        section.setTitle(request.getTitle());
        if (request.getContent() != null)      section.setContent(request.getContent());
        if (request.getDisplayOrder() != null) section.setDisplayOrder(request.getDisplayOrder());
        if (request.getVisible() != null)      section.setVisible(request.getVisible());
        if (request.getAiGenerated() != null)  section.setAiGenerated(request.getAiGenerated());
        section.setUpdatedAt(LocalDateTime.now());
        return toResponse(sectionRepository.save(section));
    }

    /**
     * Atomically updates displayOrder for all sections in the list.
     * Receives [{sectionId, displayOrder}, ...] and bulk-saves in one transaction.
     */
    @Override
    @Transactional
    public void reorderSections(List<SectionOrderDTO> newOrder) {
        // Build a lookup map for efficiency
        Map<String, Integer> orderMap = newOrder.stream()
                .collect(Collectors.toMap(SectionOrderDTO::getSectionId, SectionOrderDTO::getDisplayOrder));

        List<ResumeSection> sections = sectionRepository.findAllById(orderMap.keySet());

        if (sections.size() != orderMap.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "One or more section IDs not found");
        }

        sections.forEach(s -> {
            s.setDisplayOrder(orderMap.get(s.getSectionId()));
            s.setUpdatedAt(LocalDateTime.now());
        });

        sectionRepository.saveAll(sections);
    }

    /**
     * Batch save all sections for a resume atomically (called from builder UI).
     * Matches sections by sectionId when content fields are supplied inline.
     */
    @Override
    @Transactional
    public List<SectionResponse> bulkUpdateSections(List<UpdateSectionRequest> updates, String resumeId) {
        List<ResumeSection> existing =
                sectionRepository.findByResumeIdOrderByDisplayOrderAsc(resumeId);

        // Apply updates positionally (index-matched) — caller sends full ordered list
        for (int i = 0; i < Math.min(updates.size(), existing.size()); i++) {
            UpdateSectionRequest req = updates.get(i);
            ResumeSection sec = existing.get(i);
            if (req.getTitle() != null)        sec.setTitle(req.getTitle());
            if (req.getContent() != null)      sec.setContent(req.getContent());
            if (req.getDisplayOrder() != null) sec.setDisplayOrder(req.getDisplayOrder());
            if (req.getVisible() != null)      sec.setVisible(req.getVisible());
            if (req.getAiGenerated() != null)  sec.setAiGenerated(req.getAiGenerated());
            sec.setUpdatedAt(LocalDateTime.now());
        }

        return sectionRepository.saveAll(existing)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SectionResponse toggleVisibility(String sectionId) {
        ResumeSection section = findOrThrow(sectionId);
        section.setVisible(!section.isVisible());
        section.setUpdatedAt(LocalDateTime.now());
        return toResponse(sectionRepository.save(section));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteSection(String sectionId) {
        ResumeSection section = findOrThrow(sectionId);
        sectionRepository.delete(section);
    }

    @Override
    @Transactional
    public void deleteSectionsByResumeId(String resumeId) {
        sectionRepository.deleteByResumeId(resumeId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ResumeSection findOrThrow(String sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Section not found: " + sectionId));
    }

    private ResumeSection.SectionType parseSectionType(String type) {
        try {
            return ResumeSection.SectionType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid section type: " + type +
                            ". Valid values: SUMMARY, EXPERIENCE, EDUCATION, SKILLS, " +
                            "CERTIFICATIONS, PROJECTS, LANGUAGES, VOLUNTEER, CUSTOM");
        }
    }

    private SectionResponse toResponse(ResumeSection s) {
        return SectionResponse.builder()
                .sectionId(s.getSectionId())
                .resumeId(s.getResumeId())
                .sectionType(s.getSectionType().name())
                .title(s.getTitle())
                .content(s.getContent())
                .displayOrder(s.getDisplayOrder())
                .visible(s.isVisible())
                .aiGenerated(s.isAiGenerated())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}