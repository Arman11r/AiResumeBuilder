package com.resumeai.resume.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Resume {


    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "resume_id", updatable = false, nullable = false)
    private String resumeId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String title;

    @Column(name = "target_job_title")
    private String targetJobTitle;

    @Column(name = "template_id")
    private String templateId;

    @Column(name = "ats_score")
    private Integer atsScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.DRAFT;

    @Column(nullable = false)
    private String language = "en";

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    public enum Status { DRAFT, COMPLETE }
}