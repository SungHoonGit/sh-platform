package com.shplatform.resume.api.dto;

import java.time.LocalDateTime;

/**
 * 공유 링크 응답.
 *
 * @param documentId 공유 대상 문서 ID
 * @param token      공유 토큰 (URL 식별자)
 * @param expiresAt  만료 시각 (null이면 무기한)
 * @param createdAt  생성 시각
 */
public record ShareLinkResponse(
        Long documentId,
        String token,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
}