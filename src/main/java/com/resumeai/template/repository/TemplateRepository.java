package com.resumeai.template.repository;

import com.resumeai.template.entity.ResumeTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TemplateRepository extends JpaRepository<ResumeTemplate, String> {
    List<ResumeTemplate> findByIsActiveTrue();
    List<ResumeTemplate> findByIsPremiumFalseAndIsActiveTrue();
    List<ResumeTemplate> findByIsPremiumTrueAndIsActiveTrue();
    List<ResumeTemplate> findByCategoryAndIsActiveTrue(ResumeTemplate.Category category);
    List<ResumeTemplate> findAllByOrderByUsageCountDesc();
}