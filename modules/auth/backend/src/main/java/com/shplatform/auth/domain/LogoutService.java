package com.shplatform.auth.domain;

import com.shplatform.auth.infrastructure.TokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 로그아웃 시 access token 블랙리스트 등록과 세션 정리를 처리한다.
 *
 * <p>JWT 검증 로직을 domain 레이어로 위임해 api/ 레이어의
 * infrastructure 의존을 제거한다.
 */
@Service
public class LogoutService {

    private static final Logger log = LoggerFactory.getLogger(LogoutService.class);

    private final TokenProvider tokenProvider;
    private final TokenBlacklistService blacklistService;
    private final SessionService sessionService;

    public LogoutService(TokenProvider tokenProvider,
                         TokenBlacklistService blacklistService,
                         SessionService sessionService) {
        this.tokenProvider = tokenProvider;
        this.blacklistService = blacklistService;
        this.sessionService = sessionService;
    }

    /**
     * access token을 블랙리스트에 추가하고 연결된 세션을 정리한다.
     *
     * <p>토큰 검증 실패 등 어떠한 예외도 로그만 남기고 무시한다(이미 refresh 토큰은
     * 무효화된 뒤이므로 블랙리스트는 부가 조치).
     *
     * @param accessToken 블랙리스트에 추가할 access token
     */
    public void logoutByAccessToken(String accessToken) {
        try {
            var claims = tokenProvider.validate(accessToken);
            long remainingSeconds = (claims.exp() - System.currentTimeMillis()) / 1000;
            if (remainingSeconds > 0) {
                blacklistService.addToBlacklist(accessToken, remainingSeconds);
            }
            if (claims.sessionId() != null && !claims.sessionId().isBlank()) {
                sessionService.removeSession(claims.userId(), claims.sessionId());
            } else {
                sessionService.removeAllSessions(claims.userId());
            }
        } catch (Exception e) {
            log.debug("[LOGOUT] failed to blacklist token: {}", e.getMessage());
        }
    }
}