package com.resumeai.resume.service;

import com.resumeai.resume.dto.CreateResumeRequest;
import com.resumeai.resume.dto.ResumeResponse;
import com.resumeai.resume.dto.UpdateResumeRequest;

import java.util.List;

public interface ResumeService {
    ResumeResponse createResume(String userId, CreateResumeRequest request);
    ResumeResponse getResumeById(String resumeId);
    List<ResumeResponse> getResumesByUser(String userId);
    ResumeResponse updateResume(String resumeId, UpdateResumeRequest request);
    void deleteResume(String resumeId);
    ResumeResponse duplicateResume(String resumeId);
    ResumeResponse updateAtsScore(String resumeId, int score);
    ResumeResponse publishResume(String resumeId);
    ResumeResponse unpublishResume(String resumeId);
    List<ResumeResponse> getPublicResumes();
    void incrementViewCount(String resumeId);
}