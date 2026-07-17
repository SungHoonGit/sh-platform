package com.shplatform.auth.infrastructure.tenant;

import com.shplatform.auth.domain.tenant.TenantStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TenantRepository extends JpaRepository<TenantEntity, Long>, JpaSpecificationExecutor<TenantEntity> {
    Optional<TenantEntity> findBySlug(String slug);
    boolean existsBySlug(String slug);
    long countByStatus(TenantStatus status);
}
