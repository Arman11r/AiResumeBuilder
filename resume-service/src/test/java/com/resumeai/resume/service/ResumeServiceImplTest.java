package com.resumeai.resume.service;

import com.resumeai.resume.dto.CreateResumeRequest;
import com.resumeai.resume.dto.ResumeResponse;
import com.resumeai.resume.dto.UpdateResumeRequest;
import com.resumeai.resume.entity.Resume;
import com.resumeai.resume.exception.ResumeQuotaExceededException;
import com.resumeai.resume.repository.ResumeRepository;
import com.resumeai.resume.service.impl.ResumeServiceImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Unit tests verifying the core logic of this service.
@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeServiceImpl Tests")
class ResumeServiceImplTest {

    @Mock private ResumeRepository resumeRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private ResumeServiceImpl resumeService;

    // ── Shared helpers ────────────────────────────────────────────────────────

    private Resume buildResume(String id, String userId) {
        return Resume.builder()
                .resumeId(id)
                .userId(userId)
                .title("My Resume")
                .targetJobTitle("Software Engineer")
                .templateId("template-01")
                .language("en")
                .status(Resume.Status.DRAFT)
                .isPublic(false)
                .viewCount(0)
                .build();
    }

    /** Stub auth-service call to return a FREE subscription plan. */
    @SuppressWarnings("unchecked")
    private void stubFreeUser(String userId) {
        when(restTemplate.getForObject(contains(userId), eq(Map.class)))
                .thenReturn(Map.of("subscriptionPlan", "FREE"));
    }

    /** Stub auth-service call to return a PREMIUM subscription plan. */
    @SuppressWarnings("unchecked")
    private void stubPremiumUser(String userId) {
        when(restTemplate.getForObject(contains(userId), eq(Map.class)))
                .thenReturn(Map.of("subscriptionPlan", "PREMIUM"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createResume()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createResume()")
    class CreateResume {

        @Test
        @DisplayName("should create and return a resume for a FREE user under quota")
        void createResume_freeUserUnderQuota_returnsResponse() {
            // 1. Set up the test conditions
            CreateResumeRequest request = new CreateResumeRequest();
            request.setTitle("My Resume");
            request.setTargetJobTitle("Software Engineer");
            request.setTemplateId("template-01");

            stubFreeUser("user-001");
            when(resumeRepository.countByUserId("user-001")).thenReturn(2L);
            Resume saved = buildResume("resume-001", "user-001");
            when(resumeRepository.save(any(Resume.class))).thenReturn(saved);

            // 2. Run the method under test
            ResumeResponse response = resumeService.createResume("user-001", request);

            // 3. Verify the outcome
            assertThat(response).isNotNull();
            assertThat(response.getResumeId()).isEqualTo("resume-001");
            assertThat(response.getStatus()).isEqualTo("DRAFT");
            verify(resumeRepository).save(any(Resume.class));
        }

        @Test
        @DisplayName("should throw ResumeQuotaExceededException when FREE user hits limit")
        void createResume_freeUserAtQuota_throwsException() {
            // 1. Set up the test conditions
            CreateResumeRequest request = new CreateResumeRequest();
            request.setTitle("Resume 4");

            stubFreeUser("user-001");
            when(resumeRepository.countByUserId("user-001")).thenReturn(3L);

            // Run and verify the expected outcome
            assertThatThrownBy(() -> resumeService.createResume("user-001", request))
                    .isInstanceOf(ResumeQuotaExceededException.class);

            verify(resumeRepository, never()).save(any());
        }

        @Test
        @DisplayName("should allow PREMIUM user to create beyond free quota")
        void createResume_premiumUserOverFreeLimit_succeeds() {
            // 1. Set up the test conditions
            CreateResumeRequest request = new CreateResumeRequest();
            request.setTitle("Resume #10");

            stubPremiumUser("user-002");
            Resume saved = buildResume("resume-010", "user-002");
            when(resumeRepository.save(any(Resume.class))).thenReturn(saved);

            // 2. Run the method under test
            ResumeResponse response = resumeService.createResume("user-002", request);

            // 3. Verify the outcome
            assertThat(response.getResumeId()).isEqualTo("resume-010");
            verify(resumeRepository, never()).countByUserId(any()); // quota not checked for PREMIUM
        }

        @Test
        @DisplayName("should default language to 'en' when not specified in request")
        void createResume_noLanguage_defaultsToEn() {
            // 1. Set up the test conditions
            CreateResumeRequest request = new CreateResumeRequest();
            request.setTitle("Resume");
            // language intentionally left null

            stubFreeUser("user-001");
            when(resumeRepository.countByUserId("user-001")).thenReturn(0L);

            Resume saved = buildResume("r-001", "user-001");
            when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> {
                Resume r = inv.getArgument(0);
                assertThat(r.getLanguage()).isEqualTo("en"); // verified inside save
                return saved;
            });

            // 2. Run the method under test
            resumeService.createResume("user-001", request);

            // Assert – verified inline above
            verify(resumeRepository).save(any(Resume.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getResumeById()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getResumeById()")
    class GetResumeById {

        @Test
        @DisplayName("should return resume when it exists")
        void getResumeById_existingId_returnsResponse() {
            // 1. Set up the test conditions
            Resume resume = buildResume("resume-001", "user-001");
            when(resumeRepository.findById("resume-001")).thenReturn(Optional.of(resume));

            // 2. Run the method under test
            ResumeResponse response = resumeService.getResumeById("resume-001");

            // 3. Verify the outcome
            assertThat(response.getResumeId()).isEqualTo("resume-001");
            assertThat(response.getUserId()).isEqualTo("user-001");
        }

        @Test
        @DisplayName("should throw NOT_FOUND when resume does not exist")
        void getResumeById_nonExistingId_throwsNotFound() {
            // 1. Set up the test conditions
            when(resumeRepository.findById("ghost-id")).thenReturn(Optional.empty());

            // Run and verify the expected outcome
            assertThatThrownBy(() -> resumeService.getResumeById("ghost-id"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Resume not found");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getResumesByUser()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getResumesByUser()")
    class GetResumesByUser {

        @Test
        @DisplayName("should return list of resumes for a user")
        void getResumesByUser_existingUser_returnsList() {
            // 1. Set up the test conditions
            Resume r1 = buildResume("resume-001", "user-001");
            Resume r2 = buildResume("resume-002", "user-001");
            when(resumeRepository.findByUserId("user-001")).thenReturn(List.of(r1, r2));

            // 2. Run the method under test
            List<ResumeResponse> responses = resumeService.getResumesByUser("user-001");

            // 3. Verify the outcome
            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(ResumeResponse::getResumeId)
                    .containsExactly("resume-001", "resume-002");
        }

        @Test
        @DisplayName("should return empty list when user has no resumes")
        void getResumesByUser_noResumes_returnsEmptyList() {
            // 1. Set up the test conditions
            when(resumeRepository.findByUserId("user-002")).thenReturn(List.of());

            // 2. Run the method under test
            List<ResumeResponse> responses = resumeService.getResumesByUser("user-002");

            // 3. Verify the outcome
            assertThat(responses).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateResume()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateResume()")
    class UpdateResume {

        @Test
        @DisplayName("should update title and status when both are provided")
        void updateResume_validRequest_updatesFields() {
            // 1. Set up the test conditions
            Resume resume = buildResume("resume-001", "user-001");
            when(resumeRepository.findById("resume-001")).thenReturn(Optional.of(resume));
            when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateResumeRequest request = new UpdateResumeRequest();
            request.setTitle("Updated Title");
            request.setStatus("COMPLETE");

            // 2. Run the method under test
            ResumeResponse response = resumeService.updateResume("resume-001", request);

            // 3. Verify the outcome
            assertThat(response.getTitle()).isEqualTo("Updated Title");
            assertThat(response.getStatus()).isEqualTo("COMPLETE");
        }

        @Test
        @DisplayName("should throw BAD_REQUEST for invalid status value")
        void updateResume_invalidStatus_throwsBadRequest() {
            // 1. Set up the test conditions
            Resume resume = buildResume("resume-001", "user-001");
            when(resumeRepository.findById("resume-001")).thenReturn(Optional.of(resume));

            UpdateResumeRequest request = new UpdateResumeRequest();
            request.setStatus("PUBLISHED"); // not a valid enum

            // Run and verify the expected outcome
            assertThatThrownBy(() -> resumeService.updateResume("resume-001", request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid status");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // publishResume() / unpublishResume()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("publishResume() / unpublishResume()")
    class PublishUnpublish {

        @Test
        @DisplayName("should set isPublic=true on publish")
        void publishResume_setsPublicTrue() {
            // 1. Set up the test conditions
            Resume resume = buildResume("resume-001", "user-001");
            when(resumeRepository.findById("resume-001")).thenReturn(Optional.of(resume));
            when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));

            // 2. Run the method under test
            ResumeResponse response = resumeService.publishResume("resume-001");

            // 3. Verify the outcome
            assertThat(response.isPublic()).isTrue();
        }

        @Test
        @DisplayName("should set isPublic=false on unpublish")
        void unpublishResume_setsPublicFalse() {
            // 1. Set up the test conditions
            Resume resume = buildResume("resume-001", "user-001");
            resume.setPublic(true);
            when(resumeRepository.findById("resume-001")).thenReturn(Optional.of(resume));
            when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));

            // 2. Run the method under test
            ResumeResponse response = resumeService.unpublishResume("resume-001");

            // 3. Verify the outcome
            assertThat(response.isPublic()).isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // incrementViewCount()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("incrementViewCount()")
    class IncrementViewCount {

        @Test
        @DisplayName("should increment viewCount by 1")
        void incrementViewCount_incrementsCounter() {
            // 1. Set up the test conditions
            Resume resume = buildResume("resume-001", "user-001");
            resume.setViewCount(5);
            when(resumeRepository.findById("resume-001")).thenReturn(Optional.of(resume));
            when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));

            // 2. Run the method under test
            resumeService.incrementViewCount("resume-001");

            // 3. Verify the outcome
            assertThat(resume.getViewCount()).isEqualTo(6);
            verify(resumeRepository).save(resume);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // duplicateResume()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("duplicateResume()")
    class DuplicateResume {

        @Test
        @DisplayName("should create a copy with '(Copy)' suffix and status DRAFT")
        void duplicateResume_underQuota_createsCopy() {
            // 1. Set up the test conditions
            Resume original = buildResume("resume-001", "user-001");
            when(resumeRepository.findById("resume-001")).thenReturn(Optional.of(original));
            stubFreeUser("user-001");
            when(resumeRepository.countByUserId("user-001")).thenReturn(2L);

            Resume copy = buildResume("resume-002", "user-001");
            copy.setTitle("My Resume (Copy)");
            when(resumeRepository.save(any(Resume.class))).thenReturn(copy);

            // 2. Run the method under test
            ResumeResponse response = resumeService.duplicateResume("resume-001");

            // 3. Verify the outcome
            assertThat(response.getTitle()).isEqualTo("My Resume (Copy)");
            assertThat(response.getStatus()).isEqualTo("DRAFT");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteResume()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteResume()")
    class DeleteResume {

        @Test
        @DisplayName("should delete the resume when it exists")
        void deleteResume_existingResume_deletesFromRepo() {
            // 1. Set up the test conditions
            Resume resume = buildResume("resume-001", "user-001");
            when(resumeRepository.findById("resume-001")).thenReturn(Optional.of(resume));

            // 2. Run the method under test
            resumeService.deleteResume("resume-001");

            // 3. Verify the outcome
            verify(resumeRepository).delete(resume);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when resume does not exist")
        void deleteResume_nonExistingResume_throwsNotFound() {
            // 1. Set up the test conditions
            when(resumeRepository.findById("ghost-id")).thenReturn(Optional.empty());

            // Run and verify the expected outcome
            assertThatThrownBy(() -> resumeService.deleteResume("ghost-id"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Resume not found");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateAtsScore()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateAtsScore()")
    class UpdateAtsScore {

        @Test
        @DisplayName("should persist the new ATS score on the resume")
        void updateAtsScore_validScore_savesScore() {
            // 1. Set up the test conditions
            Resume resume = buildResume("resume-001", "user-001");
            when(resumeRepository.findById("resume-001")).thenReturn(Optional.of(resume));
            when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));

            // 2. Run the method under test
            ResumeResponse response = resumeService.updateAtsScore("resume-001", 87);

            // 3. Verify the outcome
            assertThat(response.getAtsScore()).isEqualTo(87);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getPublicResumes()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPublicResumes()")
    class GetPublicResumes {

        @Test
        @DisplayName("should return only public resumes")
        void getPublicResumes_returnsPublicList() {
            // 1. Set up the test conditions
            Resume pub1 = buildResume("resume-001", "user-001");
            pub1.setPublic(true);
            Resume pub2 = buildResume("resume-002", "user-002");
            pub2.setPublic(true);
            when(resumeRepository.findByIsPublicTrue()).thenReturn(List.of(pub1, pub2));

            // 2. Run the method under test
            List<ResumeResponse> responses = resumeService.getPublicResumes();

            // 3. Verify the outcome
            assertThat(responses)
                    .hasSize(2)
                    .allMatch(ResumeResponse::isPublic);
        }
    }
}
