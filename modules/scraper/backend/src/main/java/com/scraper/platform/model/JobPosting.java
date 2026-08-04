package com.scraper.platform.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 채용공고 저장 엔티티.
 * DB 기반 중복 체크 및 Viewer 데이터 소스로 사용.
 */
@Entity
@Table(name = "job_postings", indexes = {
    @Index(name = "idx_job_postings_config", columnList = "config_id"),
    @Index(name = "idx_job_postings_site", columnList = "site_name"),
    @Index(name = "idx_job_postings_crawled_at", columnList = "crawled_at"),
    @Index(name = "idx_job_postings_dedup_key", columnList = "dedup_key"),
    @Index(name = "idx_job_postings_url", columnList = "url(191)")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_job_postings_dedup", columnNames = {"dedup_key", "crawled_at"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private CrawlConfig config;

    @Column(name = "site_name", length = 50, nullable = false)
    private String siteName;

    @Column(name = "url", length = 500, nullable = false)
    private String url;

    @Column(name = "company", length = 200, nullable = false)
    private String company;

    @Column(name = "position", length = 300, nullable = false)
    private String position;

    @Column(name = "career", length = 100)
    private String career;

    @Column(name = "tech", length = 500)
    private String tech;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "deadline", length = 100)
    private String deadline;

    @Column(name = "dedup_key", length = 64, nullable = false)
    private String dedupKey;

    @Column(name = "crawled_at", nullable = false)
    private LocalDate crawledAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 중복 체크용 해시 생성.
     * SHA256(회사명 + 포지션 + 지역 + 사이트명)
     */
    public static String generateDedupKey(String company, String position, String location, String siteName) {
        String raw = normalize(company) + "|" + normalize(position) + "|" + normalize(location) + "|" + siteName;
        return sha256(raw);
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase().replaceAll("\\s+", "");
    }

    private static String sha256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
