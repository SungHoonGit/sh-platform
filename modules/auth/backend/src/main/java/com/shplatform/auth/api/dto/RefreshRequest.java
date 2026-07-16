package com.shplatform.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "토큰 갱신 요청")
public record RefreshRequest(
        @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzI1NiIs...")
        @NotBlank String refreshToken
) {}
