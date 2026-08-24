package com.shplatform.resume.api.dto;

import java.time.LocalDateTime;

/**
 * 파일 업로드 응답.
 *
 * @param id           업로드된 파일 ID
 * @param originalName 원본 파일명
 * @param contentType  MIME 타입
 * @param sizeBytes    바이트 크기
 */
public record FileUploadResponse(
        Long id,
        String originalName,
        String contentType,
        Long sizeBytes
) {
}
