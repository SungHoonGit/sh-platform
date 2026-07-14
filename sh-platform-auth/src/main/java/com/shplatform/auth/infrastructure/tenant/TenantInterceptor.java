package com.shplatform.auth.infrastructure.tenant;

import com.shplatform.auth.domain.tenant.Tenant;
import com.shplatform.auth.domain.tenant.TenantService;
import com.shplatform.auth.infrastructure.TokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private final TenantService tenantService;
    private final TokenProvider tokenProvider;

    public TenantInterceptor(TenantService tenantService, TokenProvider tokenProvider) {
        this.tenantService = tenantService;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler) {
        try {
            String authorization = request.getHeader("Authorization");
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return true;
            }

            String token = authorization.substring(7);
            TokenProvider.Claims claims = tokenProvider.validate(token);
            Long userId = claims.userId();
            var tenants = tenantService.getTenantsByUserId(userId);
            if (!tenants.isEmpty()) {
                TenantContext.setCurrent(tenants.get(0));
            }
        } catch (Exception e) {
            // 토큰 파싱 실패 시 테넌트 컨텍스트 없이 진행
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                               HttpServletResponse response,
                               Object handler,
                               Exception ex) {
        TenantContext.clear();
    }
}
