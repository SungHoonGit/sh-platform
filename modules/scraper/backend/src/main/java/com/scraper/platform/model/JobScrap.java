package com.scraper.platform.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 공고 스크랩(북마크) 엔티티.
 * 사용자별로 저장한 채용공고 참조. resume 모듈 지원관리에서 posting_id로 연결된다.
 */
@Entity
@Table(name = "job_scraps", uniqueConstraints = {
    @UniqueConstraint(name = "uk_job_scraps_user_posting", columnNames = {"user_id", "posting_id"})
}, indexes = {
    @Index(name = "idx_job_scraps_user", columnList = "user_id, created_at"),
    @Index(name = "idx_job_scraps_posting", columnList = "posting_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobScrap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "posting_id", nullable = false)
    private Long postingId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
