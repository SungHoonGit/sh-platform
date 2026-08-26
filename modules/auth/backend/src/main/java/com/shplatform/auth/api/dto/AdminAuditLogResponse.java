package com.shplatform.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 관리자 감사 로그 단건 응답.
 *
 * @param id           로그 ID
 * @param actorUserId  수행한 관리자 ID
 * @param action       행위 코드 (ROLE_CHANGE / DELETE_USER / FORCE_LOGOUT)
 * @param targetUserId 대상 사용자 ID
 * @param beforeValue  변경 전 값
 * @param afterValue   변경 후 값
 * @param ip           요청 IP
 * @param createdAt    일시 (yyyy-MM-dd HH:mm:ss)
 */
@Schema(description = "관리자 감사 로그")
public record AdminAuditLogResponse(
        Long id,
        Long actorUserId,
        String action,
        Long targetUserId,
        String beforeValue,
        String afterValue,
        String ip,
        String createdAt
) {}
