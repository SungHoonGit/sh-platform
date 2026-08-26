package com.shplatform.auth.infrastructure.oauth2;

import com.shplatform.auth.domain.RefreshTokenService;
import com.shplatform.auth.domain.SessionService;
import com.shplatform.auth.infrastructure.TokenProvider;
import com.shplatform.shared.config.CookieAuthorizationRequestRepository;
import com.shplatform.shared.exception.BusinessException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final SessionService sessionService;
    private final CookieAuthorizationRequestRepository authorizationRequestRepository;

    @Value("${oauth2.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public OAuth2SuccessHandler(TokenProvider tokenProvider,
                                 RefreshTokenService refreshTokenService,
                                 SessionService sessionService,
                                 CookieAuthorizationRequestRepository authorizationRequestRepository) {
        this.tokenProvider = tokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.sessionService = sessionService;
        this.authorizationRequestRepository = authorizationRequestRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();

        String refreshToken = tokenProvider.createRefreshToken();
        String ip = request.getRemoteAddr();
        String device = request.getHeader("User-Agent");

        String accessToken;
        String sessionId;
        try {
            sessionId = sessionService.createSession(oauth2User.getUserId(), ip, device);
            accessToken = tokenProvider.createAccessToken(
                    oauth2User.getUserId(), oauth2User.getEmail(), oauth2User.getRole(), sessionId);
            refreshTokenService.save(refreshToken, oauth2User.getUserId(), sessionId);
        } catch (BusinessException e) {
            log.warn("[OAUTH2] login blocked: userId={}, reason={}",
                    oauth2User.getUserId(), e.getErrorCode());
            getRedirectStrategy().sendRedirect(request, response,
                    frontendUrl + "/auth/error?message=" + encode("session_limit"));
            return;
        }

        String returnUrl = request.getParameter("returnUrl");
        if (returnUrl == null || returnUrl.isBlank()) {
            returnUrl = authorizationRequestRepository.loadRedirectCookie(request);
        }
        if (returnUrl == null || returnUrl.isBlank() || !isSafeRedirect(returnUrl)) {
            returnUrl = "/platform";
        }

        String redirectUrl = frontendUrl + "/auth/callback"
                + "?accessToken=" + encode(accessToken)
                + "&refreshToken=" + encode(refreshToken)
                + "&provider=" + encode(oauth2User.getUserInfo().getProvider())
                + "&returnUrl=" + encode(returnUrl);

        log.info("[OAUTH2] success redirect: userId={}, provider={}, sessionId={}",
                oauth2User.getUserId(), oauth2User.getUserInfo().getProvider(), sessionId);

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private boolean isSafeRedirect(String returnUrl) {
        if (returnUrl.startsWith("//")) return false;
        if (returnUrl.startsWith("http://") || returnUrl.startsWith("https://")) {
            return returnUrl.startsWith(frontendUrl);
        }
        return returnUrl.startsWith("/");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
