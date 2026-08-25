package com.shplatform.auth.domain;

import com.shplatform.shared.config.RedisRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    private static final String SESSION_KEY_PREFIX = "session:";
    private static final String USER_SESSIONS_KEY_PREFIX = "user:sessions:";

    private final RedisRepository redisRepository;

    @Value("${app.session.max-per-user:3}")
    private int maxSessionsPerUser;

    @Value("${app.session.timeout-minutes:180}")
    private int sessionTimeoutMinutes;

    @Value("${app.session.prevent-duplicate:false}")
    private boolean preventDuplicateLogin;

    public SessionService(RedisRepository redisRepository) {
        this.redisRepository = redisRepository;
    }

    /**
     * 새 세션을 생성하고 등록한다.
     *
     * @param userId 사용자 ID
     * @param ip 클라이언트 IP
     * @param device 기기 정보 (User-Agent)
     * @return 세션 ID
     */
    public String createSession(Long userId, String ip, String device) {
        Set<Object> existingSessions = redisRepository.getSetMembers(USER_SESSIONS_KEY_PREFIX + userId);
        int currentCount = existingSessions != null ? existingSessions.size() : 0;

        if (currentCount >= maxSessionsPerUser) {
            if (preventDuplicateLogin) {
                log.warn("[SESSION] login blocked: userId={}, sessions={}", userId, currentCount);
                throw new IllegalStateException("max_sessions_exceeded");
            } else {
                log.info("[SESSION] removing oldest session: userId={}", userId);
                removeOldestSession(userId, existingSessions);
            }
        }

        String sessionId = UUID.randomUUID().toString();
        String sessionKey = SESSION_KEY_PREFIX + userId + ":" + sessionId;
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        Map<String, String> sessionData = Map.of(
                "ip", ip != null ? ip : "unknown",
                "device", device != null ? device : "unknown",
                "loginAt", now,
                "lastActiveAt", now
        );

        for (Map.Entry<String, String> entry : sessionData.entrySet()) {
            redisRepository.hashPut(sessionKey, entry.getKey(), entry.getValue());
        }
        redisRepository.expire(sessionKey, sessionTimeoutMinutes * 60L, java.util.concurrent.TimeUnit.SECONDS);

        redisRepository.addToSet(USER_SESSIONS_KEY_PREFIX + userId, sessionId);
        redisRepository.expire(USER_SESSIONS_KEY_PREFIX + userId, sessionTimeoutMinutes * 60L, java.util.concurrent.TimeUnit.SECONDS);

        log.info("[SESSION] created: userId={}, sessionId={}", userId, sessionId);
        return sessionId;
    }

    /**
     * 세션의 마지막 활성 시간을 갱신한다.
     *
     * @param userId 사용자 ID
     * @param sessionId 세션 ID
     */
    public void touchSession(Long userId, String sessionId) {
        String sessionKey = SESSION_KEY_PREFIX + userId + ":" + sessionId;
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        redisRepository.hashPut(sessionKey, "lastActiveAt", now);
        redisRepository.expire(sessionKey, sessionTimeoutMinutes * 60L, java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * 특정 세션을 삭제한다.
     *
     * @param userId 사용자 ID
     * @param sessionId 세션 ID
     */
    public void removeSession(Long userId, String sessionId) {
        String sessionKey = SESSION_KEY_PREFIX + userId + ":" + sessionId;
        redisRepository.delete(sessionKey);
        redisRepository.removeFromSet(USER_SESSIONS_KEY_PREFIX + userId, sessionId);
        log.info("[SESSION] removed: userId={}, sessionId={}", userId, sessionId);
    }

    /**
     * 사용자의 모든 세션을 삭제한다.
     *
     * @param userId 사용자 ID
     */
    public void removeAllSessions(Long userId) {
        Set<Object> sessions = redisRepository.getSetMembers(USER_SESSIONS_KEY_PREFIX + userId);
        if (sessions != null) {
            for (Object sessionId : sessions) {
                String sessionKey = SESSION_KEY_PREFIX + userId + ":" + sessionId;
                redisRepository.delete(sessionKey);
            }
        }
        redisRepository.delete(USER_SESSIONS_KEY_PREFIX + userId);
        log.info("[SESSION] removed all sessions: userId={}", userId);
    }

    /**
     * 사용자의 활성 세션 수를 반환한다.
     *
     * @param userId 사용자 ID
     * @return 활성 세션 수
     */
    public int getActiveSessionCount(Long userId) {
        Long count = redisRepository.getSetSize(USER_SESSIONS_KEY_PREFIX + userId);
        return count != null ? count.intValue() : 0;
    }

    /**
     * 사용자의 활성 세션 목록을 반환한다.
     *
     * @param userId 사용자 ID
     * @return 세션 ID 목록
     */
    public Set<Object> getActiveSessions(Long userId) {
        return redisRepository.getSetMembers(USER_SESSIONS_KEY_PREFIX + userId);
    }

    private void removeOldestSession(Long userId, Set<Object> sessions) {
        if (sessions == null || sessions.isEmpty()) return;

        String oldestSessionId = null;
        String oldestLoginAt = null;

        for (Object sessionId : sessions) {
            String sessionKey = SESSION_KEY_PREFIX + userId + ":" + sessionId;
            Object loginAt = redisRepository.hashGet(sessionKey, "loginAt");
            if (loginAt != null) {
                if (oldestLoginAt == null || loginAt.toString().compareTo(oldestLoginAt) < 0) {
                    oldestLoginAt = loginAt.toString();
                    oldestSessionId = sessionId.toString();
                }
            }
        }

        if (oldestSessionId != null) {
            removeSession(userId, oldestSessionId);
        }
    }
}
