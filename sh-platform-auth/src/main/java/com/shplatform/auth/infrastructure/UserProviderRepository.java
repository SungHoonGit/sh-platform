package com.shplatform.auth.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProviderRepository extends JpaRepository<UserProviderEntity, Long> {
    Optional<UserProviderEntity> findByProviderAndProviderId(String provider, String providerId);
    List<UserProviderEntity> findByUserId(Long userId);
    boolean existsByUserIdAndProvider(Long userId, String provider);
    void deleteByUserIdAndProvider(Long userId, String provider);
}
