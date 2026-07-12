package com.shplatform.auth.infrastructure.oauth2;

import com.shplatform.auth.infrastructure.RefreshTokenEntity;
import com.shplatform.auth.infrastructure.RefreshTokenRepository;
import com.shplatform.auth.infrastructure.TokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${oauth2.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public OAuth2SuccessHandler(TokenProvider tokenProvider,
                                 RefreshTokenRepository refreshTokenRepository) {
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();

        String accessToken = tokenProvider.createAccessToken(
                oauth2User.getUserId(), oauth2User.getEmail(), oauth2User.getRole());
        String refreshToken = tokenProvider.createRefreshToken();

        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setUserId(oauth2User.getUserId());
        refreshTokenEntity.setToken(refreshToken);
        refreshTokenEntity.setExpiresAt(LocalDateTime.now()
                .plusNanos(tokenProvider.getRefreshTokenExpiration() * 1_000_000));
        refreshTokenRepository.save(refreshTokenEntity);

        String returnUrl = request.getParameter("returnUrl");
        if (returnUrl == null || returnUrl.isBlank() || !isSafeRedirect(returnUrl)) {
            returnUrl = "/";
        }

        String redirectUrl = frontendUrl + "/auth/callback"
                + "?accessToken=" + encode(accessToken)
                + "&refreshToken=" + encode(refreshToken)
                + "&provider=" + encode(oauth2User.getUserInfo().getProvider())
                + "&returnUrl=" + encode(returnUrl);

        log.info("[OAUTH2] success redirect: userId={}, provider={}", oauth2User.getUserId(),
                oauth2User.getUserInfo().getProvider());

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
