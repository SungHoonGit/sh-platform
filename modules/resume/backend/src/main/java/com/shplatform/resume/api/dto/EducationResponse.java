package com.shplatform.resume.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 학력 응답.
 *
 * @param id           학력 ID
 * @param school       학교명
 * @param major        전공
 * @param degree       학위
 * @param startDate    입학일
 * @param endDate      졸업일
 * @param status       상태
 * @param displayOrder 표시 순서
 * @param createdAt    등록 시각
 * @param updatedAt    수정 시각
 */
public record EducationResponse(
        Long id,
        String school,
        String major,
        String degree,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Integer displayOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
