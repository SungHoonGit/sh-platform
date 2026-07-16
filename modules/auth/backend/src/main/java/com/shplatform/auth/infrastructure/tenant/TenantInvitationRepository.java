package com.shplatform.auth.infrastructure.tenant;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantInvitationRepository extends JpaRepository<TenantInvitationEntity, Long> {
    Optional<TenantInvitationEntity> findByToken(String token);
    Optional<TenantInvitationEntity> findByTenantIdAndEmail(Long tenantId, String email);
}
