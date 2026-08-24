package com.shplatform.resume.api.dto;

import java.time.LocalDateTime;

/**
 * 포트폴리오 작업물 응답.
 *
 * @param id           작업물 ID
 * @param title        작업물 제목
 * @param itemType     유형 (FILE/LINK)
 * @param filePath     저장 경로 (FILE 타입)
 * @param linkUrl      외부 URL (LINK 타입)
 * @param description  설명
 * @param displayOrder 표시 순서
 * @param createdAt    등록 시각
 */
public record PortfolioItemResponse(
        Long id,
        String title,
        String itemType,
        String filePath,
        String linkUrl,
        String description,
        Integer displayOrder,
        LocalDateTime createdAt
) {
}
