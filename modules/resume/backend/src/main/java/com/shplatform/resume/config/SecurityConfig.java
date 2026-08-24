package com.shplatform.resume.config;

import com.shplatform.common.security.JwtAuthenticationFilter;
import com.shplatform.common.security.JwtTokenValidator;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtTokenValidator jwtTokenValidator) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                        "/health",
                        "/actuator/health", "/actuator/info", "/actuator/prometheus", "/actuator/metrics",
                        // Swagger UI 정적 에셋 (상대경로 참조라 백엔드 루트로 요청됨)
                        "/swagger-ui*", "/swagger-ui/**",
                        "/swagger-initializer.js", "/index.css",
                        "/favicon-32x32.png", "/favicon-16x16.png",
                        "/v3/api-docs/**", "/v3/swagger-config", "/api-docs-ui/**"
                    ).permitAll()
                    .anyRequest().authenticated()
                )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(unauthorizedEntryPoint())
                .accessDeniedHandler(forbiddenHandler())
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenValidator),
                    UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) ->
                writeJson(response, 401, "UNAUTHORIZED", "Authentication required");
    }

    private AccessDeniedHandler forbiddenHandler() {
        return (request, response, accessDeniedException) ->
                writeJson(response, 403, "FORBIDDEN", "Access denied");
    }

    private void writeJson(jakarta.servlet.http.HttpServletResponse response,
                           int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"status\":" + status + "}");
    }
}
