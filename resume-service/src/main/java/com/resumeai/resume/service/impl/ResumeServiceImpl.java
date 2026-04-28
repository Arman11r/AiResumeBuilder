package com.resumeai.resume.service.impl;

import com.resumeai.resume.dto.CreateResumeRequest;
import com.resumeai.resume.dto.ResumeResponse;
import com.resumeai.resume.dto.UpdateResumeRequest;
import com.resumeai.resume.entity.Resume;
import com.resumeai.resume.exception.ResumeQuotaExceededException;
import com.resumeai.resume.repository.ResumeRepository;
import com.resumeai.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private static final int FREE_RESUME_LIMIT = 3;

    private final ResumeRepository resumeRepository;
    private final RestTemplate restTemplate;

    @Value("${services.auth-url}")
    private String authServiceUrl;

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ResumeResponse createResume(String userId, CreateResumeRequest request) {
        enforceQuota(userId);

        Resume resume = Resume.builder()
                .userId(userId)
                .title(request.getTitle())
                .targetJobTitle(request.getTargetJobTitle())
                .templateId(request.getTemplateId())
                .language(request.getLanguage() != null ? request.getLanguage() : "en")
                .status(Resume.Status.DRAFT)
                .isPublic(false)
                .viewCount(0)
                .build();

        return toResponse(resumeRepository.save(resume));
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Override
    public ResumeResponse getResumeById(String resumeId) {
        return toResponse(findOrThrow(resumeId));
    }

    @Override
    public List<ResumeResponse> getResumesByUser(String userId) {
        return resumeRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ResumeResponse> getPublicResumes() {
        return resumeRepository.findByIsPublicTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ResumeResponse updateResume(String resumeId, UpdateResumeRequest request) {
        Resume resume = findOrThrow(resumeId);

        if (request.getTitle() != null)         resume.setTitle(request.getTitle());
        if (request.getTargetJobTitle() != null) resume.setTargetJobTitle(request.getTargetJobTitle());
        if (request.getTemplateId() != null)     resume.setTemplateId(request.getTemplateId());
        if (request.getLanguage() != null)       resume.setLanguage(request.getLanguage());
        if (request.getStatus() != null) {
            try {
                resume.setStatus(Resume.Status.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + request.getStatus());
            }
        }
        resume.setUpdatedAt(LocalDateTime.now());
        return toResponse(resumeRepository.save(resume));
    }

    @Override
    @Transactional
    public ResumeResponse updateAtsScore(String resumeId, int score) {
        Resume resume = findOrThrow(resumeId);
        resume.setAtsScore(score);
        resume.setUpdatedAt(LocalDateTime.now());
        return toResponse(resumeRepository.save(resume));
    }

    @Override
    @Transactional
    public ResumeResponse publishResume(String resumeId) {
        Resume resume = findOrThrow(resumeId);
        resume.setPublic(true);
        resume.setUpdatedAt(LocalDateTime.now());
        return toResponse(resumeRepository.save(resume));
    }

    @Override
    @Transactional
    public ResumeResponse unpublishResume(String resumeId) {
        Resume resume = findOrThrow(resumeId);
        resume.setPublic(false);
        resume.setUpdatedAt(LocalDateTime.now());
        return toResponse(resumeRepository.save(resume));
    }

    @Override
    @Transactional
    public void incrementViewCount(String resumeId) {
        Resume resume = findOrThrow(resumeId);
        resume.setViewCount(resume.getViewCount() + 1);
        resumeRepository.save(resume);
    }

    // ── Duplicate ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ResumeResponse duplicateResume(String resumeId) {
        Resume original = findOrThrow(resumeId);
        enforceQuota(original.getUserId());

        Resume copy = Resume.builder()
                .userId(original.getUserId())
                .title(original.getTitle() + " (Copy)")
                .targetJobTitle(original.getTargetJobTitle())
                .templateId(original.getTemplateId())
                .language(original.getLanguage())
                .status(Resume.Status.DRAFT)
                .isPublic(false)
                .viewCount(0)
                .build();

        return toResponse(resumeRepository.save(copy));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteResume(String resumeId) {
        Resume resume = findOrThrow(resumeId);
        resumeRepository.delete(resume);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Calls auth-service to get subscription plan, then checks resume count.
     * FREE users are limited to 3 resumes.
     */
    private void enforceQuota(String userId) {
        String plan = fetchSubscriptionPlan(userId);
        if ("FREE".equalsIgnoreCase(plan)) {
            long count = resumeRepository.countByUserId(userId);
            if (count >= FREE_RESUME_LIMIT) {
                throw new ResumeQuotaExceededException();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String fetchSubscriptionPlan(String userId) {
        try {
            String url = authServiceUrl + "/auth/users/" + userId + "/subscription";
            Map<String, String> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("subscriptionPlan")) {
                return response.get("subscriptionPlan");
            }
        } catch (Exception e) {
            log.warn("Could not reach auth-service to check subscription for user {}: {}. Defaulting to FREE.", userId, e.getMessage());
        }
        return "FREE"; // fail-safe: treat as FREE if auth-service is unreachable
    }

    private Resume findOrThrow(String resumeId) {
        return resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Resume not found: " + resumeId));
    }

    private ResumeResponse toResponse(Resume r) {
        return ResumeResponse.builder()
                .resumeId(r.getResumeId())
                .userId(r.getUserId())
                .title(r.getTitle())
                .targetJobTitle(r.getTargetJobTitle())
                .templateId(r.getTemplateId())
                .atsScore(r.getAtsScore())
                .status(r.getStatus().name())
                .language(r.getLanguage())
                .isPublic(r.isPublic())
                .viewCount(r.getViewCount())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}