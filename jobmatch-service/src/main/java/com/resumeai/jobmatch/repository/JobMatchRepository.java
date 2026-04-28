package com.resumeai.jobmatch.repository;

import com.resumeai.jobmatch.entity.JobMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobMatchRepository extends JpaRepository<JobMatch, String> {
    List<JobMatch> findByUserId(String userId);
    List<JobMatch> findByResumeId(String resumeId);
    List<JobMatch> findByUserIdAndIsBookmarkedTrue(String userId);
    List<JobMatch> findByUserIdOrderByMatchScoreDesc(String userId);
    List<JobMatch> findByMatchScoreGreaterThanOrderByMatchScoreDesc(int score);
}