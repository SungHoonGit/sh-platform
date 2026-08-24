package com.shplatform.resume.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 자격증 응답.
 *
 * @param id           자격증 ID
 * @param name         자격증명
 * @param issuer       발행기관
 * @param acquiredAt   취득일
 * @param displayOrder 표시 순서
 * @param createdAt    등록 시각
 */
public record CertificateResponse(
        Long id,
        String name,
        String issuer,
        LocalDate acquiredAt,
        Integer displayOrder,
        LocalDateTime createdAt
) {
}
