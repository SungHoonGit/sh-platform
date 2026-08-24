package com.shplatform.resume.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 지원 응답.
 *
 * @param id           지원 ID
 * @param postingId    원본 공고 ID
 * @param companyName  회사명
 * @param postingTitle 공고 제목
 * @param postingUrl   공고 URL
 * @param applyChannel 지원 경로
 * @param appliedAt    지원일
 * @param status       진행 상태
 * @param documentId   사용한 이력서 ID
 * @param memo         메모
 * @param createdAt    등록 시각
 */
public record ApplicationResponse(
        Long id,
        Long postingId,
        String companyName,
        String postingTitle,
        String postingUrl,
        String applyChannel,
        LocalDate appliedAt,
        String status,
        Long documentId,
        String memo,
        LocalDateTime createdAt
) {
}
