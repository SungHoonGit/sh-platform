package com.shplatform.resume.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 이력서 문서 생성 요청.
 *
 * @param title          문서 제목 (필수)
 * @param fromDocumentId 불러올 기존 문서 ID — 지정 시 해당 문서의 섹션 편성을 복사한다
 */
public record DocumentCreateRequest(
        @NotBlank @Size(max = 100) String title,
        Long fromDocumentId
) {
}
