package com.shplatform.auth.api.admin.dto;

import com.shplatform.auth.domain.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(
    @NotNull UserRole role
) {}
