package com.shplatform.auth.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLogEntity, Long> {

    /**
     * 감사 로그를 조건별로 조회한다 (최신순).
     *
     * @param action      행위 코드 필터 (null이면 전체)
     * @param actorUserId 수행 관리자 필터 (null이면 전체)
     * @param targetUserId 대상 사용자 필터 (null이면 전체)
     */
    @Query("""
            SELECT a FROM AdminAuditLogEntity a
            WHERE (:action IS NULL OR a.action = :action)
              AND (:actorUserId IS NULL OR a.actorUserId = :actorUserId)
              AND (:targetUserId IS NULL OR a.targetUserId = :targetUserId)
            """)
    Page<AdminAuditLogEntity> search(@Param("action") String action,
                                     @Param("actorUserId") Long actorUserId,
                                     @Param("targetUserId") Long targetUserId,
                                     Pageable pageable);
}
