package com.resumeai.export.service;

import com.resumeai.export.dto.ExportJobResponse;
import com.resumeai.export.dto.ExportRequest;
import com.resumeai.export.entity.ExportJob;
import com.resumeai.export.repository.ExportRepository;
import com.resumeai.export.service.impl.ExportServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ExportServiceImpl}.
 * All tests follow the Arrange-Act-Assert (AAA) pattern.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExportServiceImpl Tests")
class ExportServiceImplTest {

    @Mock private ExportRepository exportRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private ExportServiceImpl exportService;

    // ── Shared helpers ────────────────────────────────────────────────────────

    private ExportJob buildJob(String jobId, ExportJob.Format format, ExportJob.Status status) {
        ExportJob job = new ExportJob();
        job.setJobId(jobId);
        job.setResumeId("resume-001");
        job.setUserId("user-001");
        job.setFormat(format);
        job.setStatus(status);
        job.setRequestedAt(LocalDateTime.now());
        return job;
    }

    private ExportRequest buildRequest() {
        ExportRequest req = new ExportRequest();
        req.setResumeId("resume-001");
        req.setUserId("user-001");
        req.setTemplateId("template-01");
        return req;
    }

    /**
     * Inject @Value fields that Spring normally injects but Mockito skips.
     */
    private void injectValues() {
        ReflectionTestUtils.setField(exportService, "resumeUrl",       "http://resume-service");
        ReflectionTestUtils.setField(exportService, "sectionUrl",      "http://section-service");
        ReflectionTestUtils.setField(exportService, "localStoragePath", "/tmp/exports");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // submitPdfExport()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("submitPdfExport()")
    class SubmitPdfExport {

        @Test
        @DisplayName("should create a QUEUED PDF job and return the response")
        void submitPdfExport_underDailyLimit_returnsQueuedJob() {
            // Arrange
            injectValues();
            ExportRequest request = buildRequest();
            ExportJob savedJob = buildJob("job-001", ExportJob.Format.PDF, ExportJob.Status.QUEUED);

            when(exportRepository.countByUserIdAndRequestedAtAfter(eq("user-001"), any(LocalDateTime.class)))
                    .thenReturn(3L);
            when(exportRepository.save(any(ExportJob.class))).thenReturn(savedJob);

            // Act
            ExportJobResponse response = exportService.submitPdfExport(request);

            // Assert
            assertThat(response.getJobId()).isEqualTo("job-001");
            assertThat(response.getFormat()).isEqualTo("PDF");
            assertThat(response.getStatus()).isEqualTo("QUEUED");
            verify(exportRepository).save(any(ExportJob.class));
        }

        @Test
        @DisplayName("should throw TOO_MANY_REQUESTS when daily PDF limit is reached")
        void submitPdfExport_dailyLimitExceeded_throwsTooManyRequests() {
            // Arrange
            injectValues();
            ExportRequest request = buildRequest();

            when(exportRepository.countByUserIdAndRequestedAtAfter(eq("user-001"), any(LocalDateTime.class)))
                    .thenReturn(10L);

            // Act & Assert
            assertThatThrownBy(() -> exportService.submitPdfExport(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Daily PDF export limit reached");

            verify(exportRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // submitDocxExport()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("submitDocxExport()")
    class SubmitDocxExport {

        @Test
        @DisplayName("should create a QUEUED DOCX job without checking daily limit")
        void submitDocxExport_createsQueuedDocxJob() {
            // Arrange
            injectValues();
            ExportRequest request = buildRequest();
            ExportJob savedJob = buildJob("job-002", ExportJob.Format.DOCX, ExportJob.Status.QUEUED);
            when(exportRepository.save(any(ExportJob.class))).thenReturn(savedJob);

            // Act
            ExportJobResponse response = exportService.submitDocxExport(request);

            // Assert
            assertThat(response.getFormat()).isEqualTo("DOCX");
            assertThat(response.getStatus()).isEqualTo("QUEUED");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // submitJsonExport()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("submitJsonExport()")
    class SubmitJsonExport {

        @Test
        @DisplayName("should create a QUEUED JSON job")
        void submitJsonExport_createsQueuedJsonJob() {
            // Arrange
            injectValues();
            ExportRequest request = buildRequest();
            ExportJob savedJob = buildJob("job-003", ExportJob.Format.JSON, ExportJob.Status.QUEUED);
            when(exportRepository.save(any(ExportJob.class))).thenReturn(savedJob);

            // Act
            ExportJobResponse response = exportService.submitJsonExport(request);

            // Assert
            assertThat(response.getFormat()).isEqualTo("JSON");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getJobStatus()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getJobStatus()")
    class GetJobStatus {

        @Test
        @DisplayName("should return job response when job exists")
        void getJobStatus_existingJob_returnsResponse() {
            // Arrange
            ExportJob job = buildJob("job-001", ExportJob.Format.PDF, ExportJob.Status.COMPLETED);
            job.setFileUrl("/tmp/exports/job-001.pdf");
            when(exportRepository.findById("job-001")).thenReturn(Optional.of(job));

            // Act
            ExportJobResponse response = exportService.getJobStatus("job-001");

            // Assert
            assertThat(response.getStatus()).isEqualTo("COMPLETED");
            assertThat(response.getFileUrl()).isEqualTo("/tmp/exports/job-001.pdf");
        }

        @Test
        @DisplayName("should throw NOT_FOUND when job does not exist")
        void getJobStatus_nonExistingJob_throwsNotFound() {
            // Arrange
            when(exportRepository.findById("ghost-id")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> exportService.getJobStatus("ghost-id"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Export job not found");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getExportsByUser()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getExportsByUser()")
    class GetExportsByUser {

        @Test
        @DisplayName("should return all export jobs for the given user")
        void getExportsByUser_withJobs_returnsList() {
            // Arrange
            ExportJob job1 = buildJob("job-001", ExportJob.Format.PDF, ExportJob.Status.COMPLETED);
            ExportJob job2 = buildJob("job-002", ExportJob.Format.DOCX, ExportJob.Status.QUEUED);
            when(exportRepository.findByUserId("user-001")).thenReturn(List.of(job1, job2));

            // Act
            List<ExportJobResponse> responses = exportService.getExportsByUser("user-001");

            // Assert
            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(ExportJobResponse::getFormat)
                    .containsExactlyInAnyOrder("PDF", "DOCX");
        }

        @Test
        @DisplayName("should return empty list when user has no exports")
        void getExportsByUser_noJobs_returnsEmptyList() {
            // Arrange
            when(exportRepository.findByUserId("user-002")).thenReturn(List.of());

            // Act
            List<ExportJobResponse> responses = exportService.getExportsByUser("user-002");

            // Assert
            assertThat(responses).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteExport()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteExport()")
    class DeleteExport {

        @Test
        @DisplayName("should delete the export job when it exists")
        void deleteExport_existingJob_deletesFromRepo() {
            // Arrange
            ExportJob job = buildJob("job-001", ExportJob.Format.PDF, ExportJob.Status.COMPLETED);
            when(exportRepository.findById("job-001")).thenReturn(Optional.of(job));

            // Act
            exportService.deleteExport("job-001");

            // Assert
            verify(exportRepository).delete(job);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when export job does not exist")
        void deleteExport_nonExistingJob_throwsNotFound() {
            // Arrange
            when(exportRepository.findById("ghost-id")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> exportService.deleteExport("ghost-id"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Export job not found");
        }
    }
}
