package com.shplatform.common.security;

import com.shplatform.common.security.JwtTokenValidator.JwtClaims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * SecurityContext 에서 현재 로그인 사용자 클레임을 조회하는 헬퍼.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 현재 인증된 사용자의 account id(userId)를 반환한다.
     *
     * @return JWT sub(user id)
     * @throws IllegalStateException 인증 정보가 없거나 형식이 맞지 않을 때
     */
    public static Long currentAccountId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof JwtClaims claims)) {
            throw new IllegalStateException("No authenticated account");
        }
        return claims.userId();
    }
}
