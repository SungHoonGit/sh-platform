package com.shplatform.auth.api.dto;

public record UserResponse(
        Long id,
        String email,
        String name,
        String role,
        String provider,
        boolean emailVerified,
        String locale,
        String createdAt,
        String updatedAt
) {}
