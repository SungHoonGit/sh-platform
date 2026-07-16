package com.shplatform.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record SignupRequest(
        @Schema(description = "이메일", example = "user@example.com")
        @NotBlank @Email String email,
        @Schema(description = "비밀번호 (영문+숫자+특수문자 8~20자)", example = "Password1!")
        @NotBlank @Size(min = 8, max = 20)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()]).{8,20}$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다")
        String password,
        @Schema(description = "사용자명", example = "홍길동")
        @NotBlank @Size(min = 2, max = 20) String name
) {}
