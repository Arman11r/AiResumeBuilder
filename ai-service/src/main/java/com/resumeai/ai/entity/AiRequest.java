package com.resumeai.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiRequest {

    @Id
    @UuidGenerator
    @Column(name = "request_id", updatable = false, nullable = false)
    private String requestId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "resume_id")
    private String resumeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    private RequestType requestType;

    @Column(name = "input_prompt", columnDefinition = "TEXT", nullable = false)
    private String inputPrompt;

    @Column(name = "ai_response", columnDefinition = "LONGTEXT")
    private String aiResponse;

    @Enumerated(EnumType.STRING)
    private Model model = Model.GEMINI;

    @Column(name = "tokens_used")
    private int tokensUsed = 0;

    @Enumerated(EnumType.STRING)
    private Status status = Status.QUEUED;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum RequestType {
        SUMMARY, BULLETS, COVER_LETTER, IMPROVE, ATS, SKILLS, TAILOR, TRANSLATE
    }

    public enum Model { GPT4O, CLAUDE, GEMINI }
    public enum Status { QUEUED, COMPLETED, FAILED }
}