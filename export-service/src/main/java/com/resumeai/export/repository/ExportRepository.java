package com.resumeai.export.repository;

import com.resumeai.export.entity.ExportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ExportRepository extends JpaRepository<ExportJob, String> {
    List<ExportJob> findByUserId(String userId);
    List<ExportJob> findByResumeId(String resumeId);
    List<ExportJob> findByStatus(ExportJob.Status status);
    long countByUserIdAndRequestedAtAfter(String userId, LocalDateTime after);
    List<ExportJob> findByExpiresAtBeforeAndStatusNot(LocalDateTime dateTime, ExportJob.Status status);
}