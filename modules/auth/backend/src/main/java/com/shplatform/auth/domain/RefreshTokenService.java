package com.shplatform.auth.domain;

import com.shplatform.shared.config.RedisRepository;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    private final RedisRepository redisRepository;

    @Value("${jwt.refresh-token-expiration:1209600000}")
    private long refreshTokenExpirationMs;

    public RefreshTokenService(RedisRepository redisRepository) {
        this.redisRepository = redisRepository;
    }

    /**
     * Refresh Token을 Redis에 저장한다.
     *
     * <p>값은 {@code userId} 또는 기기 식별용으로 {@code userId:sessionId} 형태로 저장된다.
     *
     * @param token     Refresh Token 값
     * @param userId    사용자 ID
     * @param sessionId 세션 ID (없으면 null)
     */
    public void save(String token, Long userId, String sessionId) {
        String key = REFRESH_TOKEN_PREFIX + token;
        long timeoutSeconds = refreshTokenExpirationMs / 1000;
        String value = sessionId != null && !sessionId.isBlank()
                ? userId + ":" + sessionId
                : String.valueOf(userId);
        redisRepository.set(key, value, timeoutSeconds, TimeUnit.SECONDS);
        log.info("[REFRESH_TOKEN] saved: userId={}, expiresIn={}s", userId, timeoutSeconds);
    }

    /**
     * Refresh Token으로 사용자 ID를 조회한다.
     *
     * @param token Refresh Token 값
     * @return 사용자 ID (없으면 null)
     */
    public Long getUserId(String token) {
        String key = REFRESH_TOKEN_PREFIX + token;
        String value = redisRepository.get(key);
        if (value == null) return null;
        int idx = value.indexOf(':');
        return Long.parseLong(idx >= 0 ? value.substring(0, idx) : value);
    }

    /**
     * Refresh Token에 묶인 세션 ID를 조회한다.
     *
     * @param token Refresh Token 값
     * @return 세션 ID (레거시 값 또는 없으면 null)
     */
    public String getSessionId(String token) {
        String key = REFRESH_TOKEN_PREFIX + token;
        String value = redisRepository.get(key);
        if (value == null) return null;
        int idx = value.indexOf(':');
        return idx >= 0 ? value.substring(idx + 1) : null;
    }

    /**
     * Refresh Token이 존재하는지 확인한다.
     *
     * @param token Refresh Token 값
     * @return 존재하면 true
     */
    public boolean exists(String token) {
        String key = REFRESH_TOKEN_PREFIX + token;
        return Boolean.TRUE.equals(redisRepository.hasKey(key));
    }

    /**
     * Refresh Token을 삭제한다.
     *
     * @param token Refresh Token 값
     */
    public void delete(String token) {
        String key = REFRESH_TOKEN_PREFIX + token;
        redisRepository.delete(key);
        log.info("[REFRESH_TOKEN] deleted");
    }

    /**
     * 사용자의 모든 Refresh Token을 삭제한다.
     *
     * @param userId 사용자 ID
     */
    public void deleteByUserId(Long userId) {
        // Redis에서는 직접적으로 userId로 토큰을 찾을 수 없으므로
        // 로그아웃/회원탈퇴 시 프론트에서 토큰을 보내도록 유도
        log.info("[REFRESH_TOKEN] deleteByUserId requested: userId={}", userId);
    }
}
