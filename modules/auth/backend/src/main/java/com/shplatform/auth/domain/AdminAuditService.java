package com.shplatform.auth.domain;

import com.shplatform.auth.infrastructure.AdminAuditLogEntity;
import com.shplatform.auth.infrastructure.AdminAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 관리자 행위 감사 로그를 기록한다.
 *
 * <p>감사 실패는 비즈니스 흐름을 막지 않는다(로그만 남기고 무시).
 */
@Service
public class AdminAuditService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuditService.class);

    public static final String ACTION_ROLE_CHANGE = "ROLE_CHANGE";
    public static final String ACTION_DELETE_USER = "DELETE_USER";
    public static final String ACTION_FORCE_LOGOUT = "FORCE_LOGOUT";

    private final AdminAuditLogRepository repository;

    public AdminAuditService(AdminAuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * 관리자 행위를 감사 로그로 기록한다.
     *
     * @param actorUserId  수행한 관리자 ID
     * @param action       행위 코드 (ROLE_CHANGE / DELETE_USER / FORCE_LOGOUT)
     * @param targetUserId 대상 사용자 ID
     * @param beforeValue  변경 전 값 (스냅샷)
     * @param afterValue   변경 후 값 (스냅샷)
     * @param ip           요청 IP
     */
    public void record(Long actorUserId, String action, Long targetUserId,
                       String beforeValue, String afterValue, String ip) {
        try {
            repository.save(AdminAuditLogEntity.create(
                    actorUserId, action, targetUserId, beforeValue, afterValue, ip));
            log.info("[AUDIT] action={}, actor={}, target={}", action, actorUserId, targetUserId);
        } catch (Exception e) {
            log.warn("[AUDIT] failed to record: action={}, {}", action, e.getMessage());
        }
    }
}
