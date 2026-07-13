package com.shplatform.shared.config;

import com.shplatform.auth.infrastructure.TokenProvider;
import com.shplatform.auth.infrastructure.oauth2.CustomOAuth2UserService;
import com.shplatform.auth.infrastructure.oauth2.OAuth2FailureHandler;
import com.shplatform.auth.infrastructure.oauth2.OAuth2SuccessHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final TokenProvider tokenProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final RateLimiter rateLimiter;

    @Value("${oauth2.callback-base:http://localhost:8080}")
    private String callbackBase;

    @Value("${KAKAO_CLIENT_ID:}")
    private String kakaoClientId;
    @Value("${KAKAO_CLIENT_SECRET:}")
    private String kakaoClientSecret;

    @Value("${NAVER_CLIENT_ID:}")
    private String naverClientId;
    @Value("${NAVER_CLIENT_SECRET:}")
    private String naverClientSecret;

    @Value("${GOOGLE_CLIENT_ID:}")
    private String googleClientId;
    @Value("${GOOGLE_CLIENT_SECRET:}")
    private String googleClientSecret;

    @Value("${GITHUB_CLIENT_ID:}")
    private String githubClientId;
    @Value("${GITHUB_CLIENT_SECRET:}")
    private String githubClientSecret;

    public SecurityConfig(TokenProvider tokenProvider,
                          CustomOAuth2UserService customOAuth2UserService,
                          OAuth2SuccessHandler oAuth2SuccessHandler,
                          OAuth2FailureHandler oAuth2FailureHandler,
                          RateLimiter rateLimiter) {
        this.tokenProvider = tokenProvider;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.oAuth2FailureHandler = oAuth2FailureHandler;
        this.rateLimiter = rateLimiter;
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        List<ClientRegistration> registrations = buildRegistrations();

        return new ClientRegistrationRepository() {
            @Override
            public ClientRegistration findByRegistrationId(String registrationId) {
                return registrations.stream()
                    .filter(r -> r.getRegistrationId().equals(registrationId))
                    .findFirst()
                    .orElse(null);
            }
        };
    }

    private List<ClientRegistration> buildRegistrations() {
        List<ClientRegistration> registrations = new ArrayList<>();

        if (isNotBlank(kakaoClientId)) {
            registrations.add(ClientRegistration.withRegistrationId("kakao")
                .clientId(kakaoClientId)
                .clientSecret(kakaoClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(callbackBase + "/login/oauth2/code/kakao")
                .scope("account_email", "profile_nickname")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("Kakao")
                .build());
            log.info("[OAUTH2] registered: kakao");
        }

        if (isNotBlank(naverClientId)) {
            registrations.add(ClientRegistration.withRegistrationId("naver")
                .clientId(naverClientId)
                .clientSecret(naverClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(callbackBase + "/login/oauth2/code/naver")
                .scope("email", "name")
                .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
                .tokenUri("https://nid.naver.com/oauth2.0/token")
                .userInfoUri("https://openapi.naver.com/v1/nid/me")
                .userNameAttributeName("response")
                .clientName("Naver")
                .build());
            log.info("[OAUTH2] registered: naver");
        }

        if (isNotBlank(googleClientId)) {
            registrations.add(ClientRegistration.withRegistrationId("google")
                .clientId(googleClientId)
                .clientSecret(googleClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(callbackBase + "/login/oauth2/code/google")
                .scope("email", "profile")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build());
            log.info("[OAUTH2] registered: google");
        }

        if (isNotBlank(githubClientId)) {
            registrations.add(ClientRegistration.withRegistrationId("github")
                .clientId(githubClientId)
                .clientSecret(githubClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(callbackBase + "/login/oauth2/code/github")
                .scope("user:email")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("id")
                .clientName("GitHub")
                .build());
            log.info("[OAUTH2] registered: github");
        }

        log.info("[OAUTH2] total registered providers: {}", registrations.size());
        return registrations;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        var auth = http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(
                    "/api/v1/auth/signup", "/api/v1/auth/login",
                    "/api/v1/auth/refresh", "/api/v1/auth/verify-email",
                    "/api/v1/auth/verify-code", "/api/v1/auth/oauth2/**",
                    "/login/oauth2/code/**",
                    "/api/health", "/actuator/health", "/h2-console/**",
                    "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(fo -> fo.sameOrigin()))
            .addFilterBefore(rateLimiter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new JwtAuthenticationFilter(tokenProvider),
                    UsernamePasswordAuthenticationFilter.class);

        List<String> providers = new ArrayList<>();
        if (isNotBlank(kakaoClientId)) providers.add("kakao");
        if (isNotBlank(naverClientId)) providers.add("naver");
        if (isNotBlank(googleClientId)) providers.add("google");
        if (isNotBlank(githubClientId)) providers.add("github");

        if (!providers.isEmpty()) {
            auth.oauth2Login(oauth -> oauth
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler)
                .userInfoEndpoint(info -> info.userService(customOAuth2UserService))
            );
            log.info("[SECURITY] OAuth2 login enabled for: {}", providers);
        } else {
            log.info("[SECURITY] OAuth2 login disabled (no providers configured)");
        }

        return auth.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.addAllowedOrigin("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
