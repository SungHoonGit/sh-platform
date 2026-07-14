package com.scraper.platform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crawl_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CrawlData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id")
    private CrawlConfig config;

    @Column(length = 100)
    private String category;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(length = 255)
    private String title;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "source_site", length = 100)
    private String sourceSite;

    @Column(length = 100)
    private String author;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "crawled_at")
    private LocalDateTime crawledAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
