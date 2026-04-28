package com.resumeai.ai.repository;

import com.resumeai.ai.entity.AiRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AiRequestRepository extends JpaRepository<AiRequest, String> {

        List<AiRequest> findByUserIdOrderByCreatedAtDesc(String userId);

        List<AiRequest> findByResumeId(String resumeId);

        @Query("SELECT COUNT(r) FROM AiRequest r WHERE r.userId = :userId " +
                        "AND r.requestType != :requestType " +
                        "AND r.createdAt >= :startTime")
        long countContentCallsSince(@Param("userId") String userId,
                        @Param("requestType") AiRequest.RequestType requestType,
                        @Param("startTime") LocalDateTime startTime);

        @Query("SELECT COUNT(r) FROM AiRequest r WHERE r.userId = :userId " +
                        "AND r.requestType = :requestType " +
                        "AND r.createdAt >= :startTime")
        long countAtsCallsSince(@Param("userId") String userId,
                        @Param("requestType") AiRequest.RequestType requestType,
                        @Param("startTime") LocalDateTime startTime);
}