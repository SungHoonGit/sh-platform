package com.scraper.platform.config;

import com.shplatform.common.security.JwtAuthenticationFilter;
import com.shplatform.common.security.JwtTokenValidator;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
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
                    "/", "/index.html", "/favicon.ico",
                    "/assets/**", "/static/**",
                    "/api/health",
                    "/actuator/health", "/actuator/info", "/actuator/prometheus", "/actuator/metrics",
                    "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/api-docs-ui/**"
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
