package com.shplatform.resume.infrastructure;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 지원 이력 엔티티.
 * 스크래퍼 공고(job_postings)를 참조할 수 있으며, 회사/공고 정보는 스냅샷으로 보관한다.
 */
@Entity
@Table(name = "resume_applications", indexes = {
    @Index(name = "idx_resume_applications_user", columnList = "user_id, applied_at"),
    @Index(name = "idx_resume_applications_status", columnList = "user_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 원본 공고 ID (scraper_platform.job_postings, FK 없음) */
    @Column(name = "posting_id")
    private Long postingId;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "posting_title", nullable = false, length = 200)
    private String postingTitle;

    @Column(name = "posting_url", length = 500)
    private String postingUrl;

    /** PLATFORM | LINK | EMAIL | ETC */
    @Column(name = "apply_channel", nullable = false, length = 20)
    private String applyChannel;

    @Column(name = "applied_at")
    private LocalDate appliedAt;

    /** PREPARING | APPLIED | SCREEN_PASSED | INTERVIEW | OFFER | REJECTED */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** 사용한 이력서 (resume_documents.id) */
    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
