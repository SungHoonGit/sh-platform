package com.shplatform.resume.api.dto;

import java.time.LocalDateTime;

/**
 * 이력서 문서 응답.
 *
 * @param id            문서 ID
 * @param title         문서 제목
 * @param templateCode  템플릿 코드
 * @param primary       대표 문서 여부
 * @param sectionConfig 섹션 편성 JSON 문자열
 * @param createdAt     생성 시각
 * @param updatedAt     수정 시각
 */
public record DocumentResponse(
        Long id,
        String title,
        String templateCode,
        boolean primary,
        String sectionConfig,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
