package com.shplatform.auth.api.admin.dto;

import com.shplatform.auth.domain.UserRole;
import java.time.LocalDateTime;

public record UserListResponse(
    Long id,
    String name,
    String email,
    UserRole role,
    String provider,
    boolean emailVerified,
    LocalDateTime createdAt
) {}
