package com.resumeai.template.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDateTime;

@Entity
@Table(name = "resume_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResumeTemplate {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "template_id", updatable = false, nullable = false)
    private String templateId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "html_layout", columnDefinition = "LONGTEXT")
    private String htmlLayout;

    @Column(name = "css_styles", columnDefinition = "LONGTEXT")
    private String cssStyles;

    @Enumerated(EnumType.STRING)
    private Category category = Category.PROFESSIONAL;

    @Column(name = "is_premium")
    private boolean isPremium = false;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "usage_count")
    private int usageCount = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum Category {
        PROFESSIONAL, CREATIVE, MODERN, MINIMALIST, ATS_OPTIMISED
    }
}