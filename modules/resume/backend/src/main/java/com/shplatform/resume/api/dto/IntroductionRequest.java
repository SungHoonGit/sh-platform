package com.shplatform.resume.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 자기소개 항목 등록/수정 요청.
 *
 * @param title        항목 제목 (필수, 예: "지원동기")
 * @param content      내용 (필수)
 * @param displayOrder 표시 순서
 */
public record IntroductionRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank String content,
        Integer displayOrder
) {
}
