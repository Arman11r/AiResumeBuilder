package com.resumeai.jobmatch.service.impl;

import com.resumeai.jobmatch.dto.*;
import com.resumeai.jobmatch.entity.JobMatch;
import com.resumeai.jobmatch.repository.JobMatchRepository;
import com.resumeai.jobmatch.service.JobMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobMatchServiceImpl implements JobMatchService {

    private final JobMatchRepository jobMatchRepository;
    private final RestTemplate restTemplate;

    @Value("${services.resume-url}")
    private String resumeUrl;

    @Value("${services.ai-url}")
    private String aiUrl;

    @Value("${jobmatch.rapidapi.key:}")
    private String rapidApiKey;

    @Value("${jobmatch.rapidapi.jsearch-host:jsearch.p.rapidapi.com}")
    private String jsearchHost;

    @Value("${jobmatch.rapidapi.jsearch-url:https://jsearch.p.rapidapi.com/search}")
    private String jsearchUrl;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public JobMatchResponse analyzeJobFit(AnalyzeJobFitRequest request) {
        String resumeText = fetchResumeText(request.getResumeId());
        int score = computeBasicScore(resumeText, request.getJobDescription());
        String missingSkills = extractMissingKeywords(resumeText, request.getJobDescription());

        JobMatch match = JobMatch.builder()
                .resumeId(request.getResumeId())
                .userId(request.getUserId())
                .jobTitle(request.getJobTitle())
                .jobDescription(request.getJobDescription())
                .matchScore(score)
                .missingSkills(missingSkills)
                .recommendations(buildRecommendations(missingSkills, score))
                .source(JobMatch.Source.MANUAL)
                .isBookmarked(false)
                .build();

        return toResponse(jobMatchRepository.save(match));
    }

    @Override
    public List<JobMatchResponse> getMatchesByUser(String userId) {
        return jobMatchRepository.findByUserId(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<JobMatchResponse> getMatchesByResume(String resumeId) {
        return jobMatchRepository.findByResumeId(resumeId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<JobMatchResponse> getTopMatches(String userId, int limit) {
        return jobMatchRepository.findByUserIdOrderByMatchScoreDesc(userId)
                .stream().limit(limit).map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<JobMatchResponse> getBookmarkedMatches(String userId) {
        return jobMatchRepository.findByUserIdAndIsBookmarkedTrue(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public JobMatchResponse bookmarkMatch(String matchId) {
        JobMatch match = findOrThrow(matchId);
        match.setBookmarked(!match.isBookmarked());
        return toResponse(jobMatchRepository.save(match));
    }

    @Override
    @Transactional
    public List<JobMatchResponse> fetchJobsFromLinkedIn(FetchJobsRequest request) {
        log.info("LinkedIn job fetch for: {} in {}", request.getJobTitle(), request.getLocation());
        return fetchJobsFromJSearch(request, JobMatch.Source.LINKEDIN, "linkedin");
    }

    @Override
    @Transactional
    public List<JobMatchResponse> fetchJobsFromNaukri(FetchJobsRequest request) {
        log.info("Naukri job fetch for: {} in {}", request.getJobTitle(), request.getLocation());
        return fetchJobsFromJSearch(request, JobMatch.Source.NAUKRI, "naukri");
    }

    @Override
    @Transactional
    public void deleteMatch(String matchId) {
        JobMatch match = findOrThrow(matchId);
        jobMatchRepository.delete(match);
    }

    // ── JSearch API Integration ───────────────────────────────────────────────

    // Pulls real-time job listings from the JSearch API.
    // If the API key isn't set up, it gracefully falls back to using mock data so the app doesn't break.
    @SuppressWarnings("unchecked")
    private List<JobMatchResponse> fetchJobsFromJSearch(
            FetchJobsRequest request, JobMatch.Source source, String platformFilter) {

        // Use our dummy data if the API key is missing
        if (rapidApiKey == null || rapidApiKey.isBlank()) {
            log.info("No RapidAPI key configured — returning mock job data");
            return createMockJobMatches(request, source);
        }

        String resumeText = fetchResumeText(request.getResumeId());

        try {
            // Construct the search query combining title and location
            String query = request.getJobTitle()
                    + (request.getLocation() != null && !request.getLocation().isBlank()
                    ? " in " + request.getLocation() : "");

            String url = UriComponentsBuilder.fromUriString(jsearchUrl)
                    .queryParam("query", query)
                    .queryParam("page", 1)
                    .queryParam("num_pages", 1)
                    .queryParam("date_posted", "week")
                    .build().toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-RapidAPI-Key", rapidApiKey);
            headers.set("X-RapidAPI-Host", jsearchHost);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            if (response.getBody() == null || !response.getBody().containsKey("data")) {
                log.warn("JSearch returned empty response — falling back to mock");
                return createMockJobMatches(request, source);
            }

            List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
            int limit = request.getLimit() != null ? request.getLimit() : 10;

            List<JobMatch> matches = data.stream()
                    .limit(limit)
                    .map(job -> {
                        String title       = str(job, "job_title");
                        String company     = str(job, "employer_name");
                        String description = str(job, "job_description");
                        String applyUrl    = str(job, "job_apply_link");


                        String shortDesc = description != null && description.length() > 2000
                                ? description.substring(0, 2000) : description;

                        int score = computeBasicScore(resumeText, description != null ? description : "");
                        String missing = extractMissingKeywords(resumeText, description != null ? description : "");

                        return JobMatch.builder()
                                .resumeId(request.getResumeId())
                                .userId(request.getUserId())
                                .jobTitle(title + (company != null ? " at " + company : ""))
                                .companyName(company)
                                .jobDescription(shortDesc)
                                .applyUrl(applyUrl)
                                .matchScore(score)
                                .missingSkills(missing)
                                .recommendations(buildRecommendations(missing, score))
                                .source(source)
                                .isBookmarked(false)
                                .build();
                    })
                    .collect(Collectors.toList());

            List<JobMatch> saved = jobMatchRepository.saveAll(matches);
            log.info("Saved {} live job matches from JSearch for user {}", saved.size(), request.getUserId());
            return saved.stream().map(this::toResponse).collect(Collectors.toList());

        } catch (Exception ex) {
            log.error("JSearch API call failed: {} — falling back to mock data", ex.getMessage());
            return createMockJobMatches(request, source);
        }
    }

    // ── Local helpers ─────────────────────────────────────────────────────────

    private String fetchResumeText(String resumeId) {
        try {
            Map response = restTemplate.getForObject(resumeUrl + "/resumes/" + resumeId, Map.class);
            return response != null ? response.toString() : "";
        } catch (Exception e) {
            log.warn("Could not fetch resume {}: {}", resumeId, e.getMessage());
            return "";
        }
    }

    private int computeBasicScore(String resumeText, String jobDescription) {
        if (resumeText == null || resumeText.isBlank() || jobDescription == null || jobDescription.isBlank()) return 40;
        String resumeLower = resumeText.toLowerCase();
        String[] jobWords = jobDescription.toLowerCase().split("[\\s,;.!?()\"/\\\\]+");
        long total = Arrays.stream(jobWords).filter(w -> w.length() > 4).distinct().count();
        if (total == 0) return 40;
        long matched = Arrays.stream(jobWords).filter(w -> w.length() > 4 && resumeLower.contains(w)).distinct().count();
        return (int) Math.max(20, Math.min(95, (matched * 100.0 / total)));
    }

    private String extractMissingKeywords(String resumeText, String jobDescription) {
        if (resumeText == null || jobDescription == null) return "";
        String resumeLower = resumeText.toLowerCase();
        String[] jobWords = jobDescription.toLowerCase().split("[\\s,;.!?()\"/\\\\]+");
        return Arrays.stream(jobWords)
                .filter(w -> w.length() > 4)
                .filter(w -> !resumeLower.contains(w))
                .distinct()
                .limit(12)
                .collect(Collectors.joining(", "));
    }

    private String buildRecommendations(String missingSkills, int score) {
        if (score >= 80) {
            return "Great match! Tailor your summary to echo the job title exactly. Consider adding any missing keywords naturally.";
        }
        if (score >= 60) {
            return "Good fit. Add the missing skills to your Skills section and reflect them in your experience bullet points.";
        }
        return "Your resume needs some work for this role. Focus on adding the missing keywords and quantifying your achievements with numbers.";
    }

    private List<JobMatchResponse> createMockJobMatches(FetchJobsRequest request, JobMatch.Source source) {
        Object[][] mockJobs = {   // ← String[][] → Object[][]
                {"Senior " + request.getJobTitle(), "Google",    "We are seeking an experienced professional...", "Docker, Kubernetes, GCP",        82},
                {request.getJobTitle(),             "Microsoft", "Join our world-class team...",                  "Azure, TypeScript, GraphQL",      74},
                {request.getJobTitle() + " II",     "Amazon",    "We are looking for a talented individual...",  "AWS Lambda, DynamoDB, Terraform", 68},
        };

        String resumeText = fetchResumeText(request.getResumeId());
        List<JobMatch> matches = new ArrayList<>();

        for (Object[] job : mockJobs) {
            String title   = (String) job[0];
            String company = (String) job[1];
            String desc    = (String) job[2];
            String missing = (String) job[3];
            int score      = (int) job[4];

            // Calculate an actual match score against the user's resume text, if we have it
            if (!resumeText.isBlank()) {
                score = computeBasicScore(resumeText, desc);
                missing = extractMissingKeywords(resumeText, desc);
            }

            matches.add(JobMatch.builder()
                    .resumeId(request.getResumeId())
                    .userId(request.getUserId())
                    .jobTitle(title + " at " + company)
                    .companyName(company)
                    .jobDescription(desc)
                    .matchScore(score)
                    .missingSkills(missing)
                    .recommendations(buildRecommendations(missing, score))
                    .source(source)
                    .isBookmarked(false)
                    .build());
        }

        return jobMatchRepository.saveAll(matches)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private JobMatch findOrThrow(String matchId) {
        return jobMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job match not found: " + matchId));
    }

    private String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private JobMatchResponse toResponse(JobMatch m) {
        return JobMatchResponse.builder()
                .matchId(m.getMatchId())
                .resumeId(m.getResumeId())
                .userId(m.getUserId())
                .jobTitle(m.getJobTitle())
                .companyName(m.getCompanyName())
                .jobDescription(m.getJobDescription())
                .matchScore(m.getMatchScore())
                .missingSkills(m.getMissingSkills())
                .recommendations(m.getRecommendations())
                .source(m.getSource().name())
                .isBookmarked(m.isBookmarked())
                .matchedAt(m.getMatchedAt())
                .build();
    }
}