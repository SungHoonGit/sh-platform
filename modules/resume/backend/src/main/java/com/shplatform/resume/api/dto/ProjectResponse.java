package com.shplatform.resume.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 프로젝트 응답.
 *
 * @param id           프로젝트 ID
 * @param name         프로젝트명
 * @param role         담당 역할
 * @param startDate    시작일
 * @param endDate      종료일
 * @param description  프로젝트 설명
 * @param techStack    기술 스택
 * @param linkUrl      관련 링크
 * @param displayOrder 표시 순서
 * @param createdAt    등록 시각
 * @param updatedAt    수정 시각
 */
public record ProjectResponse(
        Long id,
        String name,
        String role,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        String techStack,
        String linkUrl,
        Integer displayOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
