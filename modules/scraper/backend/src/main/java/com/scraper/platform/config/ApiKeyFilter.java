package com.scraper.platform.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 외부 연동 export API(X-API-Key 헤더) 전용 인증 필터.
 * <p>JWT 없이 상수시간 비교로 API 키를 검증한다. 키 불일치/미설정 시 401을 즉시 반환한다.
 * `/export/**` 이외 경로는 본 필터를 건너뛴다.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String EXPORT_PREFIX = "/export/";
    private static final String HEADER = "X-API-Key";

    private final String apiKey;

    public ApiKeyFilter(@Value("${export.api-key:}") String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(EXPORT_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (apiKey.isEmpty()) {
            writeError(response, "EXPORT_API_KEY not configured");
            return;
        }

        String provided = request.getHeader(HEADER);
        if (provided == null || provided.isBlank() || !constantTimeEquals(apiKey, provided.trim())) {
            writeError(response, "Invalid or missing X-API-Key");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private void writeError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"" + message + "\",\"status\":401}");
    }
}