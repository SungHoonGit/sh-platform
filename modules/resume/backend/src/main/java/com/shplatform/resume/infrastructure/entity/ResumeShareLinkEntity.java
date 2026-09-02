package com.shplatform.resume.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이력서 문서 공유 링크 엔티티.
 * 문서 당 공유 링크는 0 또는 1개다 (document_id UNIQUE).
 */
@Entity
@Table(name = "resume_share_links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResumeShareLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "token", nullable = false, length = 64, unique = true)
    private String token;

    /** 만료 시각. null이면 무기한이다. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * (팩토리) 공유 링크를 생성한다.
     *
     * @param documentId 문서 ID
     * @param token      UUID 기반 토큰 (고유)
     * @param expiresAt  만료 시각 (null이면 무기한)
     * @return 생성된 엔티티 (저장 전)
     */
    public static ResumeShareLinkEntity create(Long documentId, String token, LocalDateTime expiresAt) {
        ResumeShareLinkEntity entity = new ResumeShareLinkEntity();
        entity.documentId = documentId;
        entity.token = token;
        entity.expiresAt = expiresAt;
        return entity;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}