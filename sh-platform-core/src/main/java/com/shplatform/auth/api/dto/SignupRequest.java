package com.shplatform.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 20)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()]).{8,20}$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다")
        String password,
        @NotBlank @Size(min = 2, max = 20) String name
) {}
