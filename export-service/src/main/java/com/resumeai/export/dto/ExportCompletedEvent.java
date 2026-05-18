package com.resumeai.export.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportCompletedEvent implements Serializable {
    private String jobId;
    private String userId;
    private String fileUrl;
    private String format;
    private LocalDateTime completedAt;
}
