package com.shplatform.resume.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 경력 등록/수정 요청.
 *
 * @param company      회사명 (필수)
 * @param title        직무/직급
 * @param startDate    입사일
 * @param endDate      퇴사일 (재직중이면 null)
 * @param description  주요 업무 내용
 * @param displayOrder 표시 순서
 */
public record CareerRequest(
        @NotBlank @Size(max = 100) String company,
        @Size(max = 100) String title,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        Integer displayOrder
) {
}
