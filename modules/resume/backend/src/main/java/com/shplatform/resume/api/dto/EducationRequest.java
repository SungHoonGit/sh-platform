package com.shplatform.resume.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 학력 등록/수정 요청.
 *
 * @param school       학교명 (필수)
 * @param major        전공
 * @param degree       학위
 * @param startDate    입학일
 * @param endDate      졸업일
 * @param status       상태
 * @param displayOrder 표시 순서
 */
public record EducationRequest(
        @NotBlank @Size(max = 100) String school,
        @Size(max = 100) String major,
        @Size(max = 20) String degree,
        LocalDate startDate,
        LocalDate endDate,
        @Size(max = 20) String status,
        Integer displayOrder
) {
}
