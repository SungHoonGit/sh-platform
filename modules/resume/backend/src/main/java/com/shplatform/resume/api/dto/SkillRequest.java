package com.shplatform.resume.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 스킬 등록/수정 요청.
 *
 * @param name         스킬명 (필수)
 * @param level        숙련도
 * @param category     카테고리
 * @param displayOrder 표시 순서
 */
public record SkillRequest(
        @NotBlank @Size(max = 50) String name,
        @Size(max = 20) String level,
        @Size(max = 50) String category,
        Integer displayOrder
) {
}
