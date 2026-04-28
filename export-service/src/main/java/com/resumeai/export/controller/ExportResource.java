package com.resumeai.export.controller;

import com.resumeai.export.dto.ExportJobResponse;
import com.resumeai.export.dto.ExportRequest;
import com.resumeai.export.service.ExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/exports")
@RequiredArgsConstructor
public class ExportResource {

    private final ExportService exportService;

    @PostMapping("/pdf")
    public ResponseEntity<ExportJobResponse> exportPdf(
            @Valid @RequestBody ExportRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(exportService.submitPdfExport(request));
    }

    @PostMapping("/docx")
    public ResponseEntity<ExportJobResponse> exportDocx(
            @Valid @RequestBody ExportRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(exportService.submitDocxExport(request));
    }

    @PostMapping("/json")
    public ResponseEntity<ExportJobResponse> exportJson(
            @Valid @RequestBody ExportRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(exportService.submitJsonExport(request));
    }
    @GetMapping("/download/{jobId}")
    public ResponseEntity<org.springframework.core.io.Resource> download(
            @PathVariable String jobId) throws java.io.IOException {
        ExportJobResponse job = exportService.getJobStatus(jobId);
        if (job.getFileUrl() == null) {
            return ResponseEntity.notFound().build();
        }
        java.io.File file = new java.io.File(job.getFileUrl());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        org.springframework.core.io.Resource resource =
                new org.springframework.core.io.FileSystemResource(file);
        String filename = file.getName();
        String contentType = filename.endsWith(".pdf") ? "application/pdf"
                : filename.endsWith(".docx")
                ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                : "application/json";
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .body(resource);
    }
    @GetMapping("/{jobId}/status")
    public ResponseEntity<ExportJobResponse> getStatus(@PathVariable String jobId) {
        return ResponseEntity.ok(exportService.getJobStatus(jobId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ExportJobResponse>> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(exportService.getExportsByUser(userId));
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> delete(@PathVariable String jobId) {
        exportService.deleteExport(jobId);
        return ResponseEntity.noContent().build();
    }
}