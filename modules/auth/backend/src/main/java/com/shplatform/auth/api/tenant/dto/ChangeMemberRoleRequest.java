package com.shplatform.auth.api.tenant.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeMemberRoleRequest(
        @NotBlank(message = "역할은 필수입니다.")
        String role
) {}
