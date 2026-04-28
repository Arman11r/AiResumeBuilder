package com.resumeai.section.service;

import com.resumeai.section.dto.*;

import java.util.List;

public interface SectionService {
    SectionResponse createSection(CreateSectionRequest request);
    SectionResponse getSectionById(String sectionId);
    List<SectionResponse> getSectionsByResumeId(String resumeId);
    List<SectionResponse> getSectionsByType(String resumeId, String sectionType);
    SectionResponse updateSection(String sectionId, UpdateSectionRequest request);
    void reorderSections(List<SectionOrderDTO> newOrder);
    List<SectionResponse> bulkUpdateSections(List<UpdateSectionRequest> sections, String resumeId);
    SectionResponse toggleVisibility(String sectionId);
    void deleteSection(String sectionId);
    void deleteSectionsByResumeId(String resumeId);
}