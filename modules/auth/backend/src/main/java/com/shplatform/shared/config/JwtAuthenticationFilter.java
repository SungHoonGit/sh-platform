package com.shplatform.shared.config;

import com.shplatform.auth.domain.TokenBlacklistService;
import com.shplatform.auth.infrastructure.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final TokenProvider tokenProvider;
    private final TokenBlacklistService blacklistService;

    public JwtAuthenticationFilter(TokenProvider tokenProvider, TokenBlacklistService blacklistService) {
        this.tokenProvider = tokenProvider;
        this.blacklistService = blacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            var token = header.substring(7);
            try {
                if (blacklistService.isBlacklisted(token)) {
                    log.debug("[JWT] token is blacklisted");
                    SecurityContextHolder.clearContext();
                } else {
                    var claims = tokenProvider.validate(token);
                    var authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + claims.role())
                    );
                    var authentication = new UsernamePasswordAuthenticationToken(
                            claims, null, authorities
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
