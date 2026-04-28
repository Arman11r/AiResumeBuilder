package com.resumeai.export.service;

import com.resumeai.export.dto.ExportJobResponse;
import com.resumeai.export.dto.ExportRequest;
import java.util.List;

public interface ExportService {
    ExportJobResponse submitPdfExport(ExportRequest request);
    ExportJobResponse submitDocxExport(ExportRequest request);
    ExportJobResponse submitJsonExport(ExportRequest request);
    ExportJobResponse getJobStatus(String jobId);
    List<ExportJobResponse> getExportsByUser(String userId);
    void deleteExport(String jobId);
}