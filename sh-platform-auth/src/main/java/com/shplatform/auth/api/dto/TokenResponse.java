package com.shplatform.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 응답")
public record TokenResponse(
        @Schema(description = "Access Token (JWT)", example = "eyJhbGciOiJIUzI1NiIs...")
        String accessToken,
        @Schema(description = "Refresh Token (JWT)", example = "eyJhbGciOiJIUzI1NiIs...")
        String refreshToken,
        @Schema(description = "Access Token 만료까지 남은 초", example = "3599")
        long expiresIn,
        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType
) {
    public static TokenResponse of(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, expiresIn, "Bearer");
    }
}
