package com.resumeai.section.repository;

import com.resumeai.section.entity.ResumeSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeSectionRepository extends JpaRepository<ResumeSection, String> {
    List<ResumeSection> findByResumeIdOrderByDisplayOrderAsc(String resumeId);
    List<ResumeSection> findByResumeIdAndSectionTypeOrderByDisplayOrderAsc(
            String resumeId, ResumeSection.SectionType sectionType);
    void deleteByResumeId(String resumeId);
}