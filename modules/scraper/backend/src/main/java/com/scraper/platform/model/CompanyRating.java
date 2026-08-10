package com.scraper.platform.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 기업 평점 엔티티.
 * 잡플래닛, 잡코리아, 사람인에서 수집한 기업 평점 저장.
 */
@Entity
@Table(name = "company_ratings", indexes = {
    @Index(name = "idx_company_ratings_average", columnList = "average_score"),
    @Index(name = "idx_company_ratings_updated", columnList = "last_updated_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", length = 200, nullable = false, unique = true)
    private String companyName;

    @Column(name = "jobplanet_score", precision = 2, scale = 1)
    private Double jobplanetScore;

    @Column(name = "jobkorea_score", precision = 2, scale = 1)
    private Double jobkoreaScore;

    @Column(name = "saramin_score", precision = 2, scale = 1)
    private Double saraminScore;

    @Column(name = "average_score", precision = 2, scale = 1)
    private Double averageScore;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 만료 여부 확인 (7일).
     */
    public boolean isExpired() {
        if (lastUpdatedAt == null) return true;
        return lastUpdatedAt.plusDays(7).isBefore(LocalDateTime.now());
    }

    /**
     * 평균 평점 계산.
     */
    public void calculateAverage() {
        int count = 0;
        double sum = 0;
        
        if (jobplanetScore != null) {
            sum += jobplanetScore;
            count++;
        }
        if (jobkoreaScore != null) {
            sum += jobkoreaScore;
            count++;
        }
        if (saraminScore != null) {
            sum += saraminScore;
            count++;
        }
        
        this.averageScore = count > 0 ? Math.round((sum / count) * 10.0) / 10.0 : null;
    }
}
