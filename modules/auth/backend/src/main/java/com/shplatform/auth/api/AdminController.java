package com.shplatform.auth.api;

import com.shplatform.auth.api.dto.AdminAnalyticsResponse;
import com.shplatform.auth.api.dto.AdminAuditLogResponse;
import com.shplatform.auth.api.dto.AdminSessionResponse;
import com.shplatform.auth.domain.LoginLogService;
import com.shplatform.auth.domain.SessionService;
import com.shplatform.auth.domain.AdminAuditService;
import com.shplatform.auth.domain.TokenBlacklistService;
import com.shplatform.auth.infrastructure.AdminAuditLogRepository;
import com.shplatform.shared.dto.ApiResponse;
import com.shplatform.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "관리자 API - 세션 관리, 애널리틱스")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final SessionService sessionService;
    private final LoginLogService loginLogService;
    private final TokenBlacklistService blacklistService;
    private final AdminAuditService adminAuditService;
    private final AdminAuditLogRepository adminAuditLogRepository;

    public AdminController(SessionService sessionService,
                           LoginLogService loginLogService,
                           TokenBlacklistService blacklistService,
                           AdminAuditService adminAuditService,
                           AdminAuditLogRepository adminAuditLogRepository) {
        this.sessionService = sessionService;
        this.loginLogService = loginLogService;
        this.blacklistService = blacklistService;
        this.adminAuditService = adminAuditService;
        this.adminAuditLogRepository = adminAuditLogRepository;
    }

    @GetMapping("/analytics")
    @Operation(summary = "애널리틱스 대시보드", description = "오늘의 로그인 통계를 반환한다.")
    public ResponseEntity<ApiResponse<AdminAnalyticsResponse>> getAnalytics() {
        var response = new AdminAnalyticsResponse(
                loginLogService.getTodaySuccessCount(),
                loginLogService.getTodayFailCount()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/sessions/{userId}")
    @Operation(summary = "사용자 세션 목록", description = "특정 사용자의 활성 세션 목록을 조회한다.")
    public ResponseEntity<ApiResponse<AdminSessionResponse>> getUserSessions(@PathVariable Long userId) {
        int activeCount = sessionService.getActiveSessionCount(userId);
        Set<Object> sessionIds = sessionService.getActiveSessions(userId);
        var sessionIdList = sessionIds != null
                ? sessionIds.stream().map(Object::toString).collect(Collectors.toList())
                : List.<String>of();

        var response = new AdminSessionResponse(userId, activeCount, sessionIdList);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/sessions/{userId}")
    @Operation(summary = "강제 로그아웃", description = "특정 사용자의 모든 세션을 삭제한다.")
    public ResponseEntity<ApiResponse<Void>> forceLogout(@PathVariable Long userId) {
        sessionService.removeAllSessions(userId);
        adminAuditService.record(SecurityUtils.currentAccountId(),
                AdminAuditService.ACTION_FORCE_LOGOUT, userId, null, null, null);
        return ResponseEntity.ok(ApiResponse.success("강제 로그아웃 완료", null));
    }

    @GetMapping("/login-logs")
    @Operation(summary = "로그인 이력", description = "오늘의 로그인 이력을 조회한다.")
    public ResponseEntity<ApiResponse<List<String>>> getLoginLogs(
            @RequestParam(required = false) String date
    ) {
        List<String> logs = date != null
                ? loginLogService.getLogs(date)
                : loginLogService.getTodayLogs();
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/audit")
    @Operation(summary = "감사 로그 조회", description = "관리자 행위 감사 로그를 최신순으로 조회한다. action/actor/target 필터 가능.")
    public ResponseEntity<ApiResponse<Page<AdminAuditLogResponse>>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) Long targetUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var result = adminAuditLogRepository.search(action, actorUserId, targetUserId,
                PageRequest.of(page, Math.min(size, 100)));
        var items = result.getContent().stream()
                .map(a -> new AdminAuditLogResponse(
                        a.getId(), a.getActorUserId(), a.getAction(), a.getTargetUserId(),
                        a.getBeforeValue(), a.getAfterValue(), a.getIp(),
                        a.getCreatedAt() != null
                                ? a.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                : null))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(new org.springframework.data.domain.PageImpl<>(items, result.getPageable(), result.getTotalElements())));
    }
}
