package com.shplatform.auth.infrastructure;

import com.shplatform.auth.domain.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toDomain(UserEntity entity) {
        if (entity == null) return null;
        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getName(),
                entity.getRole(),
                entity.getProvider(),
                entity.getProviderId(),
                entity.isEmailVerified(),
                entity.getLocale(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public UserEntity toEntity(User domain, String encodedPassword) {
        var entity = new UserEntity();
        entity.setEmail(domain.email());
        entity.setPassword(encodedPassword);
        entity.setName(domain.name());
        entity.setRole(domain.role());
        entity.setProvider(domain.provider());
        entity.setProviderId(domain.providerId());
        entity.setEmailVerified(domain.emailVerified());
        entity.setLocale(domain.locale());
        return entity;
    }
}
