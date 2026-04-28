package com.resumeai.ai.controller;

import com.resumeai.ai.dto.*;
import com.resumeai.ai.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiResource {

    private final AiService aiService;

    @PostMapping("/generateSummary")
    public ResponseEntity<AiResponse> generateSummary(@Valid @RequestBody AiRequestDTO request) {
        return ResponseEntity.ok(aiService.generateSummary(request));
    }

    @PostMapping("/generateBullets")
    public ResponseEntity<AiResponse> generateBullets(@Valid @RequestBody AiRequestDTO request) {
        return ResponseEntity.ok(aiService.generateBullets(request));
    }

    @PostMapping("/generateCoverLetter")
    public ResponseEntity<AiResponse> generateCoverLetter(@Valid @RequestBody AiRequestDTO request) {
        return ResponseEntity.ok(aiService.generateCoverLetter(request));
    }

    @PostMapping("/improveSection")
    public ResponseEntity<AiResponse> improveSection(@Valid @RequestBody AiRequestDTO request) {
        return ResponseEntity.ok(aiService.improveSection(request));
    }

    @PostMapping("/checkAts")
    public ResponseEntity<AtsResult> checkAts(@Valid @RequestBody AiRequestDTO request) {
        return ResponseEntity.ok(aiService.checkAtsCompatibility(request));
    }

    @PostMapping("/suggestSkills")
    public ResponseEntity<AiResponse> suggestSkills(@Valid @RequestBody AiRequestDTO request) {
        return ResponseEntity.ok(aiService.suggestSkills(request));
    }

    @PostMapping("/translate")
    public ResponseEntity<AiResponse> translate(@Valid @RequestBody AiRequestDTO request) {
        return ResponseEntity.ok(aiService.translateResume(request));
    }

    @PostMapping("/tailorResume")
    public ResponseEntity<AiResponse> tailorResume(@Valid @RequestBody AiRequestDTO request) {
        return ResponseEntity.ok(aiService.tailorResume(request));
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<AiResponse>> getHistory(@PathVariable String userId) {
        return ResponseEntity.ok(aiService.getHistory(userId));
    }

    @GetMapping("/quota/{userId}")
    public ResponseEntity<QuotaResponse> getQuota(@PathVariable String userId) {
        return ResponseEntity.ok(aiService.getQuota(userId));
    }
}