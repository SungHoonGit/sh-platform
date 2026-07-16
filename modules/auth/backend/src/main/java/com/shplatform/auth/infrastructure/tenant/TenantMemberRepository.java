package com.shplatform.auth.infrastructure.tenant;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantMemberRepository extends JpaRepository<TenantMemberEntity, Long> {
    List<TenantMemberEntity> findByTenantId(Long tenantId);
    List<TenantMemberEntity> findByUserId(Long userId);
    Optional<TenantMemberEntity> findByTenantIdAndUserId(Long tenantId, Long userId);
    boolean existsByTenantIdAndUserId(Long tenantId, Long userId);
    long countByTenantId(Long tenantId);
}
