package com.shplatform.resume.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 프로젝트 등록/수정 요청.
 *
 * @param name         프로젝트명 (필수)
 * @param role         담당 역할
 * @param startDate    시작일
 * @param endDate      종료일
 * @param description  프로젝트 설명
 * @param techStack    기술 스택
 * @param linkUrl      관련 링크
 * @param displayOrder 표시 순서
 */
public record ProjectRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String role,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        @Size(max = 300) String techStack,
        @Size(max = 300) String linkUrl,
        Integer displayOrder
) {
}
