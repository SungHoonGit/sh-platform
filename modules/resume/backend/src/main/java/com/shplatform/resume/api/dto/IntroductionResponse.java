package com.shplatform.resume.api.dto;

import java.time.LocalDateTime;

/**
 * 자기소개 항목 응답.
 *
 * @param id           항목 ID
 * @param title        항목 제목
 * @param content      내용
 * @param displayOrder 표시 순서
 * @param createdAt    등록 시각
 * @param updatedAt    수정 시각
 */
public record IntroductionResponse(
        Long id,
        String title,
        String content,
        Integer displayOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
