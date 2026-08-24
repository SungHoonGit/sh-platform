package com.shplatform.resume.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 지원 등록/수정 요청.
 *
 * @param companyName  회사명 (필수)
 * @param postingTitle 공고 제목 (필수)
 * @param postingUrl   공고 URL
 * @param applyChannel 지원 경로 (PLATFORM|LINK|EMAIL|ETC, 기본 LINK)
 * @param appliedAt    지원일
 * @param status       진행 상태 (PREPARING|APPLIED|SCREEN_PASSED|INTERVIEW|OFFER|REJECTED, 기본 PREPARING)
 * @param documentId   사용한 이력서 ID
 * @param postingId    원본 공고 ID (스크랩에서 불러온 경우)
 * @param memo         메모
 */
public record ApplicationRequest(
        @NotBlank(message = "회사명은 필수입니다") @Size(max = 100) String companyName,
        @NotBlank(message = "공고 제목은 필수입니다") @Size(max = 200) String postingTitle,
        @Size(max = 500) String postingUrl,
        String applyChannel,
        LocalDate appliedAt,
        String status,
        Long documentId,
        Long postingId,
        @Size(max = 5000) String memo
) {
}
