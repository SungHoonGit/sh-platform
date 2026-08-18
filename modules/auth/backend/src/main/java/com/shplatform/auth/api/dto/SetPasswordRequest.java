package com.shplatform.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "비밀번호 설정 요청 (소셜 전용 계정용)")
public record SetPasswordRequest(
        @Schema(description = "새 비밀번호 (영문+숫자+특수문자 8~20자)", example = "NewPass1!")
        @NotBlank @Size(min = 8, max = 20)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()]).{8,20}$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다")
        String newPassword
) {}
