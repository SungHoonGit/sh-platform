package com.shplatform.resume.api.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 경력 내 기간별 상세 항목 등록/수정 요청.
 *
 * @param title       업무/프로젝트명
 * @param startDate   시작일
 * @param endDate     종료일 (진행 중이면 생략)
 * @param description 상세 내용
 * @param displayOrder 표시 순서
 */
public record CareerItemRequest(
        @Size(max = 100) String title,
        LocalDate startDate,
        LocalDate endDate,
        @Size(max = 5000) String description,
        Integer displayOrder
) {
}