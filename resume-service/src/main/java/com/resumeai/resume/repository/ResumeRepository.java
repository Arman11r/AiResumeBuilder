package com.resumeai.resume.repository;

import com.resumeai.resume.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeRepository extends JpaRepository<Resume, String> {
    List<Resume> findByUserId(String userId);
    List<Resume> findByIsPublicTrue();
    long countByUserId(String userId);
    List<Resume> findByTemplateId(String templateId);
}