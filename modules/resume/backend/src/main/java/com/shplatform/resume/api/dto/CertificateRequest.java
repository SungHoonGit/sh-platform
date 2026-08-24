package com.shplatform.resume.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 자격증 등록/수정 요청.
 *
 * @param name         자격증명 (필수)
 * @param issuer       발행기관
 * @param acquiredAt   취득일
 * @param displayOrder 표시 순서
 */
public record CertificateRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String issuer,
        LocalDate acquiredAt,
        Integer displayOrder
) {
}
