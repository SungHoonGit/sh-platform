package com.shplatform.resume.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 경력 응답.
 *
 * @param id           경력 ID
 * @param company      회사명
 * @param title        직무/직급
 * @param startDate    입사일
 * @param endDate      퇴사일 (재직중이면 null)
 * @param description  주요 업무 내용
 * @param displayOrder 표시 순서
 * @param createdAt    등록 시각
 * @param updatedAt    수정 시각
 */
public record CareerResponse(
        Long id,
        String company,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        Integer displayOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
