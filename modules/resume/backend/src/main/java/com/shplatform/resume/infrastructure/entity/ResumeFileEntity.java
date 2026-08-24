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
 * 업로드 파일 메타데이터 엔티티.
 * 실제 바이너리는 로컬 디스크에 저장되고 이 테이블은 참조 정보만 보관한다.
 */
@Entity
@Table(name = "resume_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResumeFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /** baseDir 기준 상대 경로 ({userId}/{yyyyMM}/{uuid}.{ext}) */
    @Column(name = "stored_path", nullable = false, length = 300)
    private String storedPath;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * (팩토리) 업로드 메타데이터를 생성한다.
     *
     * @param userId        소유 사용자 ID
     * @param originalName  원본 파일명
     * @param storedPath    저장 상대 경로
     * @param contentType   MIME 타입
     * @param sizeBytes     바이트 크기
     * @return 생성된 엔티티 (저장 전)
     */
    public static ResumeFileEntity create(Long userId, String originalName, String storedPath,
                                          String contentType, long sizeBytes) {
        ResumeFileEntity entity = new ResumeFileEntity();
        entity.userId = userId;
        entity.originalName = originalName;
        entity.storedPath = storedPath;
        entity.contentType = contentType;
        entity.sizeBytes = sizeBytes;
        return entity;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
