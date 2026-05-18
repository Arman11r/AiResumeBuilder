package com.resumeai.section.controller;

import com.resumeai.section.dto.*;
import com.resumeai.section.service.SectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sections")
@RequiredArgsConstructor
public class SectionResource {

    private final SectionService sectionService;

    @PostMapping
    public ResponseEntity<SectionResponse> create(@Valid @RequestBody CreateSectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sectionService.createSection(request));
    }

    @GetMapping("/resume/{resumeId}")
    public ResponseEntity<List<SectionResponse>> getByResume(@PathVariable String resumeId) {
        return ResponseEntity.ok(sectionService.getSectionsByResumeId(resumeId));
    }

    @GetMapping("/{sectionId}")
    public ResponseEntity<SectionResponse> getById(@PathVariable String sectionId) {
        return ResponseEntity.ok(sectionService.getSectionById(sectionId));
    }

    @GetMapping("/resume/{resumeId}/type/{type}")
    public ResponseEntity<List<SectionResponse>> getByType(
            @PathVariable String resumeId,
            @PathVariable String type) {
        return ResponseEntity.ok(sectionService.getSectionsByType(resumeId, type));
    }

    @PutMapping("/{sectionId}")
    public ResponseEntity<SectionResponse> update(
            @PathVariable String sectionId,
            @RequestBody UpdateSectionRequest request) {
        return ResponseEntity.ok(sectionService.updateSection(sectionId, request));
    }

    // Allows the frontend to pass a list of IDs and their new order, saving everything at once.
    @PutMapping("/reorder")
    public ResponseEntity<Void> reorder(@Valid @RequestBody List<SectionOrderDTO> newOrder) {
        sectionService.reorderSections(newOrder);
        return ResponseEntity.noContent().build();
    }

    // Handles full-page saves from the builder UI by updating multiple sections simultaneously.
    @PutMapping("/bulk")
    public ResponseEntity<List<SectionResponse>> bulkUpdate(
            @RequestParam String resumeId,
            @RequestBody List<UpdateSectionRequest> sections) {
        return ResponseEntity.ok(sectionService.bulkUpdateSections(sections, resumeId));
    }

    @PutMapping("/{sectionId}/visibility")
    public ResponseEntity<SectionResponse> toggleVisibility(@PathVariable String sectionId) {
        return ResponseEntity.ok(sectionService.toggleVisibility(sectionId));
    }

    @DeleteMapping("/{sectionId}")
    public ResponseEntity<Void> delete(@PathVariable String sectionId) {
        sectionService.deleteSection(sectionId);
        return ResponseEntity.noContent().build();
    }

    // Triggered automatically when a parent resume is deleted.
    @DeleteMapping("/resume/{resumeId}")
    public ResponseEntity<Void> deleteByResume(@PathVariable String resumeId) {
        sectionService.deleteSectionsByResumeId(resumeId);
        return ResponseEntity.noContent().build();
    }
}