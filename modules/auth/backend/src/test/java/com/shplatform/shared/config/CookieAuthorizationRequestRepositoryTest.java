package com.shplatform.shared.config;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.http.Cookie;
import java.util.Collections;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@DisplayName("CookieAuthorizationRequestRepository 테스트")
class CookieAuthorizationRequestRepositoryTest {

    private final CookieAuthorizationRequestRepository repository =
            new CookieAuthorizationRequestRepository();

    @Test
    @DisplayName("OAuth2 인가 요청을 쿠키에 저장 후 정상 로드한다")
    void saveAndLoadAuthorizationRequest() {
        OAuth2AuthorizationRequest authorizationRequest = createAuthorizationRequest();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(authorizationRequest, request, response);

        Cookie cookie = response.getCookie(CookieAuthorizationRequestRepository.COOKIE_NAME);
        assertNotNull(cookie, "OAUTH2 인가 요청 쿠키가 생성되어야 한다");

        MockHttpServletRequest loadRequest = new MockHttpServletRequest();
        loadRequest.setCookies(cookie);

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(loadRequest);

        assertNotNull(loaded, "쿠키에서 인가 요청을 복원할 수 있어야 한다");
        assertEquals(authorizationRequest.getAuthorizationUri(), loaded.getAuthorizationUri());
        assertEquals(authorizationRequest.getClientId(), loaded.getClientId());
        assertEquals(authorizationRequest.getRedirectUri(), loaded.getRedirectUri());
        assertEquals(authorizationRequest.getScopes(), loaded.getScopes());
        assertEquals(authorizationRequest.getState(), loaded.getState());
    }

    @Test
    @DisplayName("UnmodifiableSet scopes도 역직렬화된다 (allowlist 함정 회귀 테스트)")
    void roundTripWithUnmodifiableSetScopes() {
        OAuth2AuthorizationRequest authorizationRequest = createAuthorizationRequest();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(authorizationRequest, request, response);

        Cookie cookie = response.getCookie(CookieAuthorizationRequestRepository.COOKIE_NAME);
        MockHttpServletRequest loadRequest = new MockHttpServletRequest();
        loadRequest.setCookies(cookie);

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(loadRequest);

        assertNotNull(loaded, "UnmodifiableSet scopes가 포함된 요청도 역직렬화되어야 한다");
        assertEquals(authorizationRequest.getScopes(), loaded.getScopes());
    }

    @Test
    @DisplayName("쿠키가 없으면 null을 반환한다")
    void loadWithoutCookieReturnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

        assertNull(loaded);
    }

    @Test
    @DisplayName("null 요청 저장 시 쿠키를 제거한다")
    void saveNullRemovesCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        Cookie stale = new Cookie(CookieAuthorizationRequestRepository.COOKIE_NAME, "stale-value");
        request.setCookies(stale);

        repository.saveAuthorizationRequest(null, request, response);

        Cookie expired = response.getCookie(CookieAuthorizationRequestRepository.COOKIE_NAME);
        assertNotNull(expired);
        assertEquals(0, expired.getMaxAge());
    }

    private OAuth2AuthorizationRequest createAuthorizationRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://provider.example.com/oauth2/authorize")
                .clientId("client-id")
                .redirectUri("http://localhost:8080/login/oauth2/code/provider")
                .scopes(Collections.unmodifiableSet(new LinkedHashSet<>(
                        java.util.List.of("openid", "profile", "email"))))
                .state("state-value")
                .build();
    }
}
