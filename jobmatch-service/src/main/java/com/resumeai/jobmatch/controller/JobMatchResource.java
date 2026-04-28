package com.resumeai.jobmatch.controller;

import com.resumeai.jobmatch.dto.*;
import com.resumeai.jobmatch.service.JobMatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/job-matches")
@RequiredArgsConstructor
public class JobMatchResource {

    private final JobMatchService jobMatchService;

    @PostMapping("/analyze")
    public ResponseEntity<JobMatchResponse> analyze(
            @Valid @RequestBody AnalyzeJobFitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobMatchService.analyzeJobFit(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<JobMatchResponse>> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(jobMatchService.getMatchesByUser(userId));
    }

    @GetMapping("/resume/{resumeId}")
    public ResponseEntity<List<JobMatchResponse>> getByResume(@PathVariable String resumeId) {
        return ResponseEntity.ok(jobMatchService.getMatchesByResume(resumeId));
    }

    @GetMapping("/top/{userId}")
    public ResponseEntity<List<JobMatchResponse>> getTopMatches(
            @PathVariable String userId,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(jobMatchService.getTopMatches(userId, limit));
    }

    @GetMapping("/bookmarked/{userId}")
    public ResponseEntity<List<JobMatchResponse>> getBookmarked(@PathVariable String userId) {
        return ResponseEntity.ok(jobMatchService.getBookmarkedMatches(userId));
    }

    @PutMapping("/{matchId}/bookmark")
    public ResponseEntity<JobMatchResponse> bookmark(@PathVariable String matchId) {
        return ResponseEntity.ok(jobMatchService.bookmarkMatch(matchId));
    }

    @PostMapping("/fetch-linkedin")
    public ResponseEntity<List<JobMatchResponse>> fetchLinkedIn(
            @Valid @RequestBody FetchJobsRequest request) {
        return ResponseEntity.ok(jobMatchService.fetchJobsFromLinkedIn(request));
    }

    @PostMapping("/fetch-naukri")
    public ResponseEntity<List<JobMatchResponse>> fetchNaukri(
            @Valid @RequestBody FetchJobsRequest request) {
        return ResponseEntity.ok(jobMatchService.fetchJobsFromNaukri(request));
    }

    @DeleteMapping("/{matchId}")
    public ResponseEntity<Void> delete(@PathVariable String matchId) {
        jobMatchService.deleteMatch(matchId);
        return ResponseEntity.noContent().build();
    }
}