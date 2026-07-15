package com.shplatform.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 프로필 응답")
public record UserResponse(
        @Schema(description = "사용자 ID") Long id,
        @Schema(description = "이메일", example = "user@example.com") String email,
        @Schema(description = "사용자명", example = "홍길동") String name,
        @Schema(description = "권한", example = "ROLE_USER") String role,
        @Schema(description = "OAuth2 provider (일반가입=null)", example = "kakao") String provider,
        @Schema(description = "이메일 인증 여부") boolean emailVerified,
        @Schema(description = "로케일", example = "ko_KR") String locale,
        @Schema(description = "가입일시", example = "2025-01-01 12:00:00") String createdAt,
        @Schema(description = "수정일시", example = "2025-01-01 12:00:00") String updatedAt
) {}
