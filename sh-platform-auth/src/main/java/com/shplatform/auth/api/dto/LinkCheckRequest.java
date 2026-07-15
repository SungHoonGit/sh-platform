package com.shplatform.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OAuth2 연결 확인 요청")
public record LinkCheckRequest(
        @Schema(description = "OAuth2 provider", example = "kakao") String provider,
        @Schema(description = "OAuth2 provider 사용자 ID") String providerId
) {}
