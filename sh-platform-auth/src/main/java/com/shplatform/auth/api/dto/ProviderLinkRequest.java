package com.shplatform.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OAuth2 계정 연결 요청")
public record ProviderLinkRequest(
        @Schema(description = "OAuth2 provider", example = "kakao") String provider,
        @Schema(description = "OAuth2 provider 사용자 ID") String providerId,
        @Schema(description = "OAuth2 provider 이메일", example = "kakao@example.com") String providerEmail
) {}
