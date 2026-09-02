package com.shplatform.resume.api.dto;

import jakarta.validation.constraints.Future;
import java.time.LocalDateTime;

/**
 * 공유 링크 생성 요청.
 *
 * @param expiresAt 만료 시각 (지정 시 미래 시각이어야 하며, null이면 무기한)
 */
public record ShareLinkCreateRequest(
        @Future LocalDateTime expiresAt
) {
}