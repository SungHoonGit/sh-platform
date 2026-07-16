package com.shplatform.auth.api.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank(message = "테넌트 이름은 필수입니다.")
        @Size(max = 100, message = "테넌트 이름은 100자 이내여야 합니다.")
        String name,

        @NotBlank(message = "슬러그는 필수입니다.")
        @Pattern(regexp = "^[a-z0-9][a-z0-9\\-]{1,48}[a-z0-9]$",
                 message = "슬러그는 소문자, 숫자, 하이픈만 사용 가능하며 3~50자여야 합니다.")
        String slug
) {}
