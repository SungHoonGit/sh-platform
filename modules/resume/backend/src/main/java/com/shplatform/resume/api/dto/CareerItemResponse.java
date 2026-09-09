package com.shplatform.resume.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 경력 내 기간별 상세 항목 응답.
 *
 * @param id          항목 ID
 * @param title       업무/프로젝트명
 * @param startDate   시작일
 * @param endDate     종료일 (진행 중이면 null)
 * @param description 상세 내용
 * @param displayOrder 표시 순서
 * @param createdAt   등록 시각
 */
public record CareerItemResponse(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        Integer displayOrder,
        LocalDateTime createdAt
) {
}