package com.scraper.platform.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crawl_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 500)
    private String query;

    @Column(name = "career_level", length = 50)
    @Builder.Default
    private String careerLevel = "경력";

    @Column(name = "career_from")
    @Builder.Default
    private Integer careerFrom = 0;

    @Column(name = "career_to")
    @Builder.Default
    private Integer careerTo = 10;

    @Column(columnDefinition = "JSON")
    private String sites;

    @Column(name = "include_titles", columnDefinition = "JSON")
    private String includeTitles;

    @Column(name = "exclude_titles", columnDefinition = "JSON")
    private String excludeTitles;

    @Column(length = 100)
    @Builder.Default
    private String schedule = "0 9 * * *";

    @Column(name = "retention_days")
    @Builder.Default
    private Integer retentionDays = 30;

    @Column(name = "max_per_site")
    @Builder.Default
    private Integer maxPerSite = 15;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
