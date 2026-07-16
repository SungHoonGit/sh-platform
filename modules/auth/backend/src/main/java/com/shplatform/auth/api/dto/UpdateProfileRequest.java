package com.shplatform.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "프로필 수정 요청")
public record UpdateProfileRequest(
        @Schema(description = "사용자명 (2~20자)", example = "홍길동")
        @Size(min = 2, max = 20) String name,
        @Schema(description = "로케일", example = "ko_KR")
        @Size(max = 10) String locale
) {}
