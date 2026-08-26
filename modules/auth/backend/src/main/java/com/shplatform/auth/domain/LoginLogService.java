package com.shplatform.auth.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shplatform.auth.infrastructure.LoginLogEntity;
import com.shplatform.auth.infrastructure.LoginLogRepository;
import com.shplatform.shared.config.RedisRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoginLogService {

    private static final Logger log = LoggerFactory.getLogger(LoginLogService.class);
    private static final String LOGIN_LOG_PREFIX = "login:log:";
    private static final long LOG_RETENTION_DAYS = 90;

    private final RedisRepository redisRepository;
    private final ObjectMapper objectMapper;
    private final LoginLogRepository loginLogRepository;

    public LoginLogService(RedisRepository redisRepository, ObjectMapper objectMapper,
                           LoginLogRepository loginLogRepository) {
        this.redisRepository = redisRepository;
        this.objectMapper = objectMapper;
        this.loginLogRepository = loginLogRepository;
    }

    /**
     * 로그인 이력을 기록한다. (Redis 실시간용 + MariaDB 감사용 이중 기록)
     *
     * @param userId  사용자 ID
     * @param email   이메일
     * @param ip      클라이언트 IP
     * @param device  기기 정보
     * @param success 로그인 성공 여부
     */
    public void logLogin(Long userId, String email, String ip, String device, boolean success) {
        try {
            String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String key = LOGIN_LOG_PREFIX + date;

            LoginLogEntry entry = new LoginLogEntry(
                    userId != null ? userId : 0L,
                    email != null ? email : "unknown",
                    ip != null ? ip : "unknown",
                    device != null ? device : "unknown",
                    success,
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );

            String json = objectMapper.writeValueAsString(entry);
            redisRepository.leftPush(key, json);
            redisRepository.expire(key, LOG_RETENTION_DAYS * 24 * 60 * 60, TimeUnit.SECONDS);

            log.debug("[LOGIN_LOG] recorded: email={}, success={}", email, success);
        } catch (Exception e) {
            log.warn("[LOGIN_LOG] failed to record (redis): {}", e.getMessage());
        }
        try {
            loginLogRepository.save(LoginLogEntity.create(userId, email, ip, device, success));
        } catch (Exception e) {
            log.warn("[LOGIN_LOG] failed to record (db): {}", e.getMessage());
        }
    }

    /**
     * 오늘의 로그인 이력을 조회한다.
     *
     * @return 로그인 이력 목록
     */
    public List<String> getTodayLogs() {
        String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        return getLogs(date);
    }

    /**
     * 특정 날짜의 로그인 이력을 조회한다.
     *
     * @param date 날짜 (yyyy-MM-dd)
     * @return 로그인 이력 목록
     */
    public List<String> getLogs(String date) {
        String key = LOGIN_LOG_PREFIX + date;
        Collection<Object> logs = redisRepository.getList(key, 0, -1);
        if (logs == null) return List.of();
        return logs.stream().map(Object::toString).toList();
    }

    /**
     * 오늘의 로그인 성공 횟수를 반환한다.
     *
     * @return 성공 횟수
     */
    public long getTodaySuccessCount() {
        return getLogs(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .stream()
                .filter(json -> json.contains("\"success\":true"))
                .count();
    }

    /**
     * 오늘의 로그인 실패 횟수를 반환한다.
     *
     * @return 실패 횟수
     */
    public long getTodayFailCount() {
        return getLogs(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .stream()
                .filter(json -> json.contains("\"success\":false"))
                .count();
    }

    public record LoginLogEntry(
            Long userId,
            String email,
            String ip,
            String device,
            boolean success,
            String timestamp
    ) {}
}
