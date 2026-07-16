package com.shplatform.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimiter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private final ConcurrentHashMap<String, RateBucket> buckets = new ConcurrentHashMap<>();

    private static final int LOGIN_MAX = 5;
    private static final int VERIFY_MAX = 3;
    private static final int GENERAL_MAX = 30;
    private static final long WINDOW_MS = 60_000;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = resolveKey(request);
        int maxAttempts = resolveMaxAttempts(request);

        RateBucket bucket = buckets.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new RateBucket(maxAttempts);
            }
            return existing;
        });

        if (!bucket.tryAcquire()) {
            log.warn("[RATE_LIMIT] blocked: key={}, uri={}", key, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"code\":\"RATE_LIMITED\",\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\",\"data\":null}");
            return;
        }

        filterChain.doFilter(request, response);
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
            return System.currentTimeMillis() - createdAt > WINDOW_MS;
        }
    }
}
