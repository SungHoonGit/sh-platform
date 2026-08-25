package com.shplatform.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimiter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private final RedisRepository redisRepository;
    private final ConcurrentHashMap<String, RateBucket> fallbackBuckets = new ConcurrentHashMap<>();
    private boolean redisAvailable = true;

    private static final int LOGIN_MAX = 5;
    private static final int VERIFY_MAX = 3;
    private static final int GENERAL_MAX = 30;
    private static final long WINDOW_SECONDS = 60;
    private static final String KEY_PREFIX = "ratelimit:";

    public RateLimiter(RedisRepository redisRepository) {
        this.redisRepository = redisRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = resolveKey(request);
        int maxAttempts = resolveMaxAttempts(request);

        boolean allowed;
        if (redisAvailable) {
            allowed = tryAcquireRedis(key, maxAttempts);
        } else {
            allowed = tryAcquireFallback(key, maxAttempts);
        }

        if (!allowed) {
            log.warn("[RATE_LIMIT] blocked: key={}, uri={}", key, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"code\":\"RATE_LIMITED\",\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\",\"data\":null}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean tryAcquireRedis(String key, int maxAttempts) {
        try {
            String redisKey = KEY_PREFIX + key;
            Long count = redisRepository.increment(redisKey);

            if (count == null) {
                redisAvailable = false;
                log.warn("[RATE_LIMIT] Redis unavailable, falling back to in-memory");
                return tryAcquireFallback(key, maxAttempts);
            }

            if (count == 1) {
                redisRepository.expire(redisKey, WINDOW_SECONDS, TimeUnit.SECONDS);
            }

            return count <= maxAttempts;
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("[RATE_LIMIT] Redis error, falling back to in-memory: {}", e.getMessage());
            return tryAcquireFallback(key, maxAttempts);
        }
    }

    private boolean tryAcquireFallback(String key, int maxAttempts) {
        RateBucket bucket = fallbackBuckets.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new RateBucket(maxAttempts);
            }
            return existing;
        });
        return bucket.tryAcquire();
    }

    private String resolveKey(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();
        return ip + ":" + uri;
    }

    private int resolveMaxAttempts(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.contains("/login")) return LOGIN_MAX;
        if (uri.contains("/verify")) return VERIFY_MAX;
        return GENERAL_MAX;
    }

    private static class RateBucket {
        private final int maxAttempts;
        private final long createdAt;
        private final AtomicInteger count;

        RateBucket(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            this.createdAt = System.currentTimeMillis();
            this.count = new AtomicInteger(0);
        }

        boolean tryAcquire() {
            return count.incrementAndGet() <= maxAttempts;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > TimeUnit.SECONDS.toMillis(WINDOW_SECONDS);
        }
    }
}
