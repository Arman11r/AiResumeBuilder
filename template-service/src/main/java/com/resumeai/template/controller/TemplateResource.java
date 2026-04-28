package com.resumeai.template.controller;

import com.resumeai.template.dto.CreateTemplateRequest;
import com.resumeai.template.dto.TemplateResponse;
import com.resumeai.template.service.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
public class TemplateResource {

    private final TemplateService templateService;

    @PostMapping
    public ResponseEntity<TemplateResponse> create(@Valid @RequestBody CreateTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.createTemplate(request));
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<TemplateResponse> getById(@PathVariable String templateId) {
        return ResponseEntity.ok(templateService.getById(templateId));
    }

    @GetMapping
    public ResponseEntity<List<TemplateResponse>> getAll() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    @GetMapping("/free")
    public ResponseEntity<List<TemplateResponse>> getFree() {
        return ResponseEntity.ok(templateService.getFreeTemplates());
    }

    @GetMapping("/premium")
    public ResponseEntity<List<TemplateResponse>> getPremium() {
        return ResponseEntity.ok(templateService.getPremiumTemplates());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<TemplateResponse>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(templateService.getByCategory(category));
    }

    @GetMapping("/popular")
    public ResponseEntity<List<TemplateResponse>> getPopular() {
        return ResponseEntity.ok(templateService.getPopularTemplates());
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<TemplateResponse> update(@PathVariable String templateId,
                                                   @RequestBody CreateTemplateRequest request) {
        return ResponseEntity.ok(templateService.updateTemplate(templateId, request));
    }

    @PutMapping("/{templateId}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable String templateId) {
        templateService.deactivateTemplate(templateId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{templateId}/usage")
    public ResponseEntity<Void> incrementUsage(@PathVariable String templateId) {
        templateService.incrementUsage(templateId);
        return ResponseEntity.noContent().build();
    }
}