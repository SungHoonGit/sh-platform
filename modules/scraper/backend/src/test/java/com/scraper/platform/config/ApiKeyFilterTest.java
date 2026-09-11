package com.scraper.platform.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ApiKeyFilter 단위 테스트 — export 경로 인증·비인증 분기를 검증한다.
 */
@DisplayName("ApiKeyFilter 테스트")
class ApiKeyFilterTest {

    @Nested
    @DisplayName("shouldNotFilter")
    class ShouldNotFilter {

        @Test
        @DisplayName("/export/ 로 시작하면 필터를 적용한다")
        void exportPath_appliesFilter() {
            ApiKeyFilter filter = new ApiKeyFilter("secret");
            HttpServletRequest req = request("/export/job-postings");
            assertFalse(filter.shouldNotFilter(req));
        }

        @Test
        @DisplayName("비 export 경로는 필터를 건너뛴다")
        void nonExportPath_skipsFilter() {
            ApiKeyFilter filter = new ApiKeyFilter("secret");
            HttpServletRequest req = request("/job-postings");
            assertTrue(filter.shouldNotFilter(req));
        }
    }

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternal {

        @Test
        @DisplayName("일치하는 API 키면 체인을 진행한다")
        void matchingKey_passesChain() throws Exception {
            ApiKeyFilter filter = new ApiKeyFilter("secret-key");
            HttpServletRequest req = request("/export/job-postings");
            HttpServletResponse res = mock(HttpServletResponse.class);
            FilterChain chain = mock(FilterChain.class);
            when(req.getHeader("X-API-Key")).thenReturn("secret-key");

            filter.doFilter(req, res, chain);

            verify(chain).doFilter(req, res);
        }

        @Test
        @DisplayName("잘못된 API 키면 401을 반환하고 체인을 진행하지 않는다")
        void wrongKey_returns401_andSkipsChain() throws Exception {
            ApiKeyFilter filter = new ApiKeyFilter("secret-key");
            HttpServletRequest req = request("/export/job-postings");
            HttpServletResponse res = response();
            FilterChain chain = mock(FilterChain.class);
            when(req.getHeader("X-API-Key")).thenReturn("wrong");

            filter.doFilter(req, res, chain);

            verify(res).setStatus(401);
            verify(res).setContentType("application/json");
            verify(chain, never()).doFilter(req, res);
        }

        @Test
        @DisplayName("API 키가 미설정이면 401을 반환한다")
        void unconfiguredKey_returns401() throws Exception {
            ApiKeyFilter filter = new ApiKeyFilter("");
            HttpServletRequest req = request("/export/job-postings");
            HttpServletResponse res = response();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(req, res, chain);

            verify(res).setStatus(401);
            verify(chain, never()).doFilter(req, res);
        }

        @Test
        @DisplayName("API 키 헤더가 없으면 401을 반환한다")
        void missingHeader_returns401() throws Exception {
            ApiKeyFilter filter = new ApiKeyFilter("secret-key");
            HttpServletRequest req = request("/export/job-postings");
            HttpServletResponse res = response();
            FilterChain chain = mock(FilterChain.class);
            when(req.getHeader("X-API-Key")).thenReturn(null);

            filter.doFilter(req, res, chain);

            verify(res).setStatus(401);
            verify(chain, never()).doFilter(req, res);
        }
    }

    private HttpServletRequest request(String uri) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn(uri);
        return req;
    }

    private HttpServletResponse response() throws java.io.IOException {
        HttpServletResponse res = mock(HttpServletResponse.class);
        when(res.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        return res;
    }
}