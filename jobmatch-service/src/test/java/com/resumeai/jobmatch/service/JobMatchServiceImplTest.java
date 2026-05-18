package com.resumeai.jobmatch.service;

import com.resumeai.jobmatch.dto.AnalyzeJobFitRequest;
import com.resumeai.jobmatch.dto.FetchJobsRequest;
import com.resumeai.jobmatch.dto.JobMatchResponse;
import com.resumeai.jobmatch.entity.JobMatch;
import com.resumeai.jobmatch.repository.JobMatchRepository;
import com.resumeai.jobmatch.service.impl.JobMatchServiceImpl;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Unit tests verifying the core logic of this service.
@ExtendWith(MockitoExtension.class)
@DisplayName("JobMatchServiceImpl Tests")
class JobMatchServiceImplTest {

    @Mock private JobMatchRepository jobMatchRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private JobMatchServiceImpl jobMatchService;

    // ── Shared helpers ────────────────────────────────────────────────────────

    private JobMatch buildMatch(String matchId, String userId, int score, boolean bookmarked) {
        return JobMatch.builder()
                .matchId(matchId)
                .resumeId("resume-001")
                .userId(userId)
                .jobTitle("Senior Software Engineer at Google")
                .companyName("Google")
                .jobDescription("We need Java skills...")
                .matchScore(score)
                .missingSkills("kubernetes, terraform")
                .recommendations("Great match! Add missing skills.")
                .source(JobMatch.Source.MANUAL)
                .isBookmarked(bookmarked)
                .build();
    }

    private AnalyzeJobFitRequest buildAnalyzeRequest() {
        AnalyzeJobFitRequest req = new AnalyzeJobFitRequest();
        req.setResumeId("resume-001");
        req.setUserId("user-001");
        req.setJobTitle("Senior Java Developer");
        req.setJobDescription("We need Java, Spring Boot, microservices, Docker, Kubernetes.");
        return req;
    }

    private void injectValues() {
        ReflectionTestUtils.setField(jobMatchService, "resumeUrl",    "http://resume-service");
        ReflectionTestUtils.setField(jobMatchService, "aiUrl",        "http://ai-service");
        ReflectionTestUtils.setField(jobMatchService, "rapidApiKey",  "");
        ReflectionTestUtils.setField(jobMatchService, "jsearchHost",  "jsearch.p.rapidapi.com");
        ReflectionTestUtils.setField(jobMatchService, "jsearchUrl",   "https://jsearch.p.rapidapi.com/search");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // analyzeJobFit()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("analyzeJobFit()")
    class AnalyzeJobFit {

        @Test
        @DisplayName("should analyse job fit and save a JobMatch record")
        void analyzeJobFit_validRequest_savesAndReturnsResponse() {
            // 1. Set up the test conditions
            AnalyzeJobFitRequest request = buildAnalyzeRequest();

            // Resume service returns empty (no resume text) — score falls back to minimum
            when(restTemplate.getForObject(anyString(), eq(java.util.Map.class))).thenReturn(null);

            JobMatch saved = buildMatch("match-001", "user-001", 40, false);
            when(jobMatchRepository.save(any(JobMatch.class))).thenReturn(saved);

            // 2. Run the method under test
            JobMatchResponse response = jobMatchService.analyzeJobFit(request);

            // 3. Verify the outcome
            assertThat(response.getMatchId()).isEqualTo("match-001");
            assertThat(response.getUserId()).isEqualTo("user-001");
            verify(jobMatchRepository).save(any(JobMatch.class));
        }

        @Test
        @DisplayName("should compute a higher score when resume contains job keywords")
        void analyzeJobFit_resumeContainsKeywords_givesHigherScore() {
            // 1. Set up the test conditions
            AnalyzeJobFitRequest request = buildAnalyzeRequest();

            // Return a resume text that contains job keywords
            when(restTemplate.getForObject(anyString(), eq(java.util.Map.class)))
                    .thenReturn(java.util.Map.of(
                            "title", "Java Developer Resume",
                            "content", "Java Spring Boot microservices Docker Kubernetes AWS"
                    ));

            when(jobMatchRepository.save(any(JobMatch.class))).thenAnswer(inv -> {
                JobMatch m = inv.getArgument(0);
                // Score should be > 40 (minimum fallback)
                assertThat(m.getMatchScore()).isGreaterThan(40);
                return buildMatch("match-002", "user-001", m.getMatchScore(), false);
            });

            // 2. Run the method under test
            JobMatchResponse response = jobMatchService.analyzeJobFit(request);

            // 3. Verify the outcome
            assertThat(response).isNotNull();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getMatchesByUser()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMatchesByUser()")
    class GetMatchesByUser {

        @Test
        @DisplayName("should return all job matches for a user")
        void getMatchesByUser_existingUser_returnsList() {
            // 1. Set up the test conditions
            JobMatch m1 = buildMatch("match-001", "user-001", 82, false);
            JobMatch m2 = buildMatch("match-002", "user-001", 65, false);
            when(jobMatchRepository.findByUserId("user-001")).thenReturn(List.of(m1, m2));

            // 2. Run the method under test
            List<JobMatchResponse> responses = jobMatchService.getMatchesByUser("user-001");

            // 3. Verify the outcome
            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(JobMatchResponse::getMatchId)
                    .containsExactly("match-001", "match-002");
        }

        @Test
        @DisplayName("should return empty list when user has no matches")
        void getMatchesByUser_noMatches_returnsEmptyList() {
            // 1. Set up the test conditions
            when(jobMatchRepository.findByUserId("user-002")).thenReturn(List.of());

            // 2. Run the method under test
            List<JobMatchResponse> responses = jobMatchService.getMatchesByUser("user-002");

            // 3. Verify the outcome
            assertThat(responses).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getMatchesByResume()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMatchesByResume()")
    class GetMatchesByResume {

        @Test
        @DisplayName("should return matches associated with a specific resume")
        void getMatchesByResume_existingResume_returnsList() {
            // 1. Set up the test conditions
            JobMatch m1 = buildMatch("match-001", "user-001", 78, false);
            when(jobMatchRepository.findByResumeId("resume-001")).thenReturn(List.of(m1));

            // 2. Run the method under test
            List<JobMatchResponse> responses = jobMatchService.getMatchesByResume("resume-001");

            // 3. Verify the outcome
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getResumeId()).isEqualTo("resume-001");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getTopMatches()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTopMatches()")
    class GetTopMatches {

        @Test
        @DisplayName("should return at most 'limit' matches ordered by score descending")
        void getTopMatches_withLimit_returnsLimitedList() {
            // 1. Set up the test conditions
            JobMatch m1 = buildMatch("m-001", "user-001", 90, false);
            JobMatch m2 = buildMatch("m-002", "user-001", 75, false);
            JobMatch m3 = buildMatch("m-003", "user-001", 60, false);
            when(jobMatchRepository.findByUserIdOrderByMatchScoreDesc("user-001"))
                    .thenReturn(List.of(m1, m2, m3));

            // 2. Run the method under test
            List<JobMatchResponse> responses = jobMatchService.getTopMatches("user-001", 2);

            // 3. Verify the outcome
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getMatchScore()).isEqualTo(90);
            assertThat(responses.get(1).getMatchScore()).isEqualTo(75);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getBookmarkedMatches()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getBookmarkedMatches()")
    class GetBookmarkedMatches {

        @Test
        @DisplayName("should return only bookmarked matches for the user")
        void getBookmarkedMatches_withBookmarks_returnsOnlyBookmarked() {
            // 1. Set up the test conditions
            JobMatch bookmarked = buildMatch("m-001", "user-001", 82, true);
            when(jobMatchRepository.findByUserIdAndIsBookmarkedTrue("user-001"))
                    .thenReturn(List.of(bookmarked));

            // 2. Run the method under test
            List<JobMatchResponse> responses = jobMatchService.getBookmarkedMatches("user-001");

            // 3. Verify the outcome
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).isBookmarked()).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // bookmarkMatch()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("bookmarkMatch()")
    class BookmarkMatch {

        @Test
        @DisplayName("should toggle bookmark from false to true")
        void bookmarkMatch_unbookmarked_becomesBookmarked() {
            // 1. Set up the test conditions
            JobMatch match = buildMatch("m-001", "user-001", 82, false);
            when(jobMatchRepository.findById("m-001")).thenReturn(Optional.of(match));
            when(jobMatchRepository.save(any(JobMatch.class))).thenAnswer(inv -> inv.getArgument(0));

            // 2. Run the method under test
            JobMatchResponse response = jobMatchService.bookmarkMatch("m-001");

            // 3. Verify the outcome
            assertThat(response.isBookmarked()).isTrue();
        }

        @Test
        @DisplayName("should toggle bookmark from true to false")
        void bookmarkMatch_bookmarked_becomesUnbookmarked() {
            // 1. Set up the test conditions
            JobMatch match = buildMatch("m-001", "user-001", 82, true);
            when(jobMatchRepository.findById("m-001")).thenReturn(Optional.of(match));
            when(jobMatchRepository.save(any(JobMatch.class))).thenAnswer(inv -> inv.getArgument(0));

            // 2. Run the method under test
            JobMatchResponse response = jobMatchService.bookmarkMatch("m-001");

            // 3. Verify the outcome
            assertThat(response.isBookmarked()).isFalse();
        }

        @Test
        @DisplayName("should throw NOT_FOUND when match does not exist")
        void bookmarkMatch_nonExistingMatch_throwsNotFound() {
            // 1. Set up the test conditions
            when(jobMatchRepository.findById("ghost-id")).thenReturn(Optional.empty());

            // Run and verify the expected outcome
            assertThatThrownBy(() -> jobMatchService.bookmarkMatch("ghost-id"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Job match not found");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // fetchJobsFromLinkedIn() — no API key (mock data path)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fetchJobsFromLinkedIn() – mock fallback")
    class FetchJobsFromLinkedIn {

        @Test
        @DisplayName("should return 3 mock matches when no RapidAPI key is configured")
        void fetchJobsFromLinkedIn_noApiKey_returnsMockData() {
            // 1. Set up the test conditions
            injectValues(); // rapidApiKey = ""
            FetchJobsRequest request = new FetchJobsRequest();
            request.setResumeId("resume-001");
            request.setUserId("user-001");
            request.setJobTitle("Java Developer");
            request.setLocation("Remote");

            when(restTemplate.getForObject(anyString(), eq(java.util.Map.class))).thenReturn(null);
            when(jobMatchRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            // 2. Run the method under test
            List<JobMatchResponse> responses = jobMatchService.fetchJobsFromLinkedIn(request);

            // 3. Verify the outcome
            assertThat(responses)
                    .hasSize(3)
                    .allMatch(r -> r.getSource().equals("LINKEDIN"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // fetchJobsFromNaukri() — no API key (mock data path)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fetchJobsFromNaukri() – mock fallback")
    class FetchJobsFromNaukri {

        @Test
        @DisplayName("should return 3 mock matches tagged as NAUKRI source")
        void fetchJobsFromNaukri_noApiKey_returnsMockData() {
            // 1. Set up the test conditions
            injectValues();
            FetchJobsRequest request = new FetchJobsRequest();
            request.setResumeId("resume-001");
            request.setUserId("user-001");
            request.setJobTitle("Backend Engineer");
            request.setLocation("Bangalore");

            when(restTemplate.getForObject(anyString(), eq(java.util.Map.class))).thenReturn(null);
            when(jobMatchRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            // 2. Run the method under test
            List<JobMatchResponse> responses = jobMatchService.fetchJobsFromNaukri(request);

            // 3. Verify the outcome
            assertThat(responses)
                    .hasSize(3)
                    .allMatch(r -> r.getSource().equals("NAUKRI"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteMatch()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteMatch()")
    class DeleteMatch {

        @Test
        @DisplayName("should delete the match when it exists")
        void deleteMatch_existingMatch_deletesFromRepo() {
            // 1. Set up the test conditions
            JobMatch match = buildMatch("m-001", "user-001", 82, false);
            when(jobMatchRepository.findById("m-001")).thenReturn(Optional.of(match));

            // 2. Run the method under test
            jobMatchService.deleteMatch("m-001");

            // 3. Verify the outcome
            verify(jobMatchRepository).delete(match);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when match does not exist")
        void deleteMatch_nonExistingMatch_throwsNotFound() {
            // 1. Set up the test conditions
            when(jobMatchRepository.findById("ghost-id")).thenReturn(Optional.empty());

            // Run and verify the expected outcome
            assertThatThrownBy(() -> jobMatchService.deleteMatch("ghost-id"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Job match not found");
        }
    }
}
