package com.resumeai.resume.controller;

import com.resumeai.resume.dto.CreateResumeRequest;
import com.resumeai.resume.dto.ResumeResponse;
import com.resumeai.resume.dto.UpdateResumeRequest;
import com.resumeai.resume.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class ResumeResource {

    private final ResumeService resumeService;

    @PostMapping
    public ResponseEntity<ResumeResponse> create(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateResumeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resumeService.createResume(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> getById(@PathVariable String id) {
        ResumeResponse resume = resumeService.getResumeById(id);
        resumeService.incrementViewCount(id);
        return ResponseEntity.ok(resume);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ResumeResponse>> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(resumeService.getResumesByUser(userId));
    }

    @GetMapping("/public")
    public ResponseEntity<List<ResumeResponse>> getPublic() {
        return ResponseEntity.ok(resumeService.getPublicResumes());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResumeResponse> update(
            @PathVariable String id,
            @RequestBody UpdateResumeRequest request) {
        return ResponseEntity.ok(resumeService.updateResume(id, request));
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<ResumeResponse> publish(@PathVariable String id) {
        return ResponseEntity.ok(resumeService.publishResume(id));
    }

    @PutMapping("/{id}/unpublish")
    public ResponseEntity<ResumeResponse> unpublish(@PathVariable String id) {
        return ResponseEntity.ok(resumeService.unpublishResume(id));
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<ResumeResponse> duplicate(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resumeService.duplicateResume(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        resumeService.deleteResume(id);
        return ResponseEntity.noContent().build();
    }

    // ── Inter-service: called by ai-service to update ATS score ──────────────

    @PutMapping("/{id}/ats-score")
    public ResponseEntity<ResumeResponse> updateAtsScore(
            @PathVariable String id,
            @RequestParam int score) {
        return ResponseEntity.ok(resumeService.updateAtsScore(id, score));
    }
}