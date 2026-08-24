package com.shplatform.resume.api.dto;

import java.time.LocalDateTime;

/**
 * 스킬 응답.
 *
 * @param id           스킬 ID
 * @param name         스킬명
 * @param level        숙련도
 * @param category     카테고리
 * @param displayOrder 표시 순서
 * @param createdAt    등록 시각
 */
public record SkillResponse(
        Long id,
        String name,
        String level,
        String category,
        Integer displayOrder,
        LocalDateTime createdAt
) {
}
