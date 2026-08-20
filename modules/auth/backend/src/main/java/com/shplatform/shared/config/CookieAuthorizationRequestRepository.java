package com.shplatform.shared.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * OAuth2 인가 요청을 HttpSession이 아닌 쿠키에 저장하는 리포지토리.
 *
 * <p>{@code SessionCreationPolicy.STATELESS} 환경에서도 OAuth2 로그인이 동작하도록
 * 인가 요청(특히 state 파라미터)을 브라우저 쿠키에 보관한다.
 * Spring Security 6.4부터 기본 제공되던 CookieOAuth2AuthorizationRequestRepository가
 * 제거되어 직접 구현한다.
 */
public class CookieAuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final Logger log = LoggerFactory.getLogger(CookieAuthorizationRequestRepository.class);

    public static final String COOKIE_NAME = "OAUTH2_AUTHORIZATION_REQUEST";
    public static final String REDIRECT_COOKIE_NAME = "OAUTH2_RETURN_URL";
    private static final int COOKIE_MAX_AGE = 300;

    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new OAuth2ClientJackson2Module());
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("org.springframework.security.")
                        .allowIfSubType("java.util.")
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return objectMapper;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Cookie cookie = findCookie(request);
        if (cookie == null) {
            return null;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cookie.getValue());
            return OBJECT_MAPPER.readValue(decoded, OAuth2AuthorizationRequest.class);
        } catch (Exception e) {
            log.warn("[OAUTH2] failed to load authorization request from cookie: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }
        try {
            byte[] encoded = OBJECT_MAPPER.writeValueAsBytes(authorizationRequest);
            String value = Base64.getUrlEncoder().withoutPadding().encodeToString(encoded);
            Cookie cookie = new Cookie(COOKIE_NAME, value);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(COOKIE_MAX_AGE);
            if (request.isSecure()) {
                cookie.setSecure(true);
                cookie.setAttribute("SameSite", "Lax");
            }
            response.addCookie(cookie);

            String returnUrl = request.getParameter("redirect");
            if (returnUrl != null && !returnUrl.isBlank()) {
                Cookie redirectCookie = new Cookie(REDIRECT_COOKIE_NAME, returnUrl);
                redirectCookie.setPath("/");
                redirectCookie.setMaxAge(COOKIE_MAX_AGE);
                if (request.isSecure()) {
                    redirectCookie.setSecure(true);
                    redirectCookie.setAttribute("SameSite", "Lax");
                }
                response.addCookie(redirectCookie);
            }
        } catch (Exception e) {
            log.warn("[OAUTH2] failed to save authorization request to cookie: {}", e.getMessage());
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
        Cookie cookie = findCookie(request);
        if (cookie != null) {
            Cookie expired = new Cookie(COOKIE_NAME, "");
            expired.setPath("/");
            expired.setMaxAge(0);
            response.addCookie(expired);
        }
        removeRedirectCookie(request, response);
        return authRequest;
    }

    /**
     * OAuth2 인증 시작 시 저장한 returnUrl 쿠키를 읽는다.
     *
     * @param request 서블릿 요청
     * @return 저장된 returnUrl, 없으면 {@code null}
     */
    public String loadRedirectCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (REDIRECT_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void removeRedirectCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return;
        }
        for (Cookie cookie : cookies) {
            if (REDIRECT_COOKIE_NAME.equals(cookie.getName())) {
                Cookie expired = new Cookie(REDIRECT_COOKIE_NAME, "");
                expired.setPath("/");
                expired.setMaxAge(0);
                response.addCookie(expired);
            }
        }
    }

    private Cookie findCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }
}
