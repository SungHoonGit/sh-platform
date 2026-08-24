package com.shplatform.resume.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이력서 문서(뷰 정의) 엔티티.
 * 마스터 데이터(항목 테이블)를 어떻게 보여줄지 정의하며 실제 항목 데이터는 갖지 않는다.
 */
@Entity
@Table(name = "resume_documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResumeDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "template_code", nullable = false, length = 20)
    private String templateCode;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    /** 섹션 편성 JSON 배열 ([{"key","included","order"}, ...]) */
    @Lob
    @Column(name = "section_config", nullable = false)
    private String sectionConfig;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * (팩토리) 문서를 생성한다.
     *
     * @param userId        소유 사용자 ID
     * @param title         문서 제목
     * @param templateCode  템플릿 코드 (CLASSIC 등)
     * @param isPrimary     대표 문서 여부
     * @param sectionConfig 섹션 편성 JSON 문자열
     * @return 생성된 엔티티 (저장 전)
     */
    public static ResumeDocumentEntity create(Long userId, String title, String templateCode,
                                              boolean isPrimary, String sectionConfig) {
        ResumeDocumentEntity entity = new ResumeDocumentEntity();
        entity.userId = userId;
        entity.title = title;
        entity.templateCode = templateCode;
        entity.primary = isPrimary;
        entity.sectionConfig = sectionConfig;
        return entity;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public void updateSectionConfig(String sectionConfig) {
        this.sectionConfig = sectionConfig;
    }

    public void markPrimary() {
        this.primary = true;
    }

    public void unmarkPrimary() {
        this.primary = false;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
