package com.resumeai.export.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class ExportJobResponse {
    private String jobId;
    private String resumeId;
    private String userId;
    private String format;
    private String status;
    private String fileUrl;
    private Integer fileSizeKb;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;
}