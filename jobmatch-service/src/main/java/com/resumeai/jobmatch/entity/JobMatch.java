package com.resumeai.jobmatch.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_matches")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobMatch {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "match_id", updatable = false, nullable = false)
    private String matchId;

    @Column(name = "resume_id", nullable = false)
    private String resumeId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "apply_url", length = 1000)
    private String applyUrl;

    @Column(name = "job_description", columnDefinition = "TEXT")
    private String jobDescription;

    @Column(name = "match_score")
    private int matchScore = 0;

    @Column(name = "missing_skills", columnDefinition = "TEXT")
    private String missingSkills;

    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @Enumerated(EnumType.STRING)
    private Source source = Source.MANUAL;

    @Column(name = "matched_at", updatable = false)
    private LocalDateTime matchedAt;

    @Column(name = "is_bookmarked")
    private boolean isBookmarked = false;

    @PrePersist
    public void prePersist() {
        this.matchedAt = LocalDateTime.now();
    }

    public enum Source { LINKEDIN, NAUKRI, MANUAL }
}