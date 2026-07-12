package com.shplatform.auth.domain;

import java.time.LocalDateTime;

public record User(
        Long id,
        String email,
        String name,
        UserRole role,
        String provider,
        String providerId,
        boolean emailVerified,
        String locale,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static User createLocal(String email, String name) {
        return new User(
                null, email, name, UserRole.USER,
                "LOCAL", null, false, "ko",
                null, null
        );
    }

    public User verifyEmail() {
        return new User(
                id, email, name, role, provider, providerId,
                true, locale, createdAt, updatedAt
        );
    }
}
