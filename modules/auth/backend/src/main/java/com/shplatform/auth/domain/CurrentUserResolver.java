package com.shplatform.auth.domain;

import com.shplatform.auth.infrastructure.TokenProvider;
import com.shplatform.auth.infrastructure.oauth2.CustomOAuth2User;
import com.shplatform.shared.exception.BusinessException;
import com.shplatform.shared.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 인증 주체(principal)에서 현재 사용자 ID를 추출한다.
 *
 * <p>JWT(JWT 인증 필터의 {@code TokenProvider.Claims}) 또는 OAuth2({@code CustomOAuth2User})
 * 인증 방식 모두를 지원한다. api/ 레이어 대신 domain에서 인프라 타입을 직접 참조해
 * 레이어 규칙(api → infrastructure 금지) 위반을 방지한다.
 */
@Component
public class CurrentUserResolver {

    /**
     * 인증 주체에서 사용자 ID를 추출한다.
     *
     * @param principal 인증 필터/핸들러가 주입한 AuthenticationPrincipal
     * @return 사용자 ID
     * @throws BusinessException UNAUTHORIZED 인증된 사용자를 찾을 수 없는 경우
     */
    public Long resolveUserId(Object principal) {
        if (principal instanceof TokenProvider.Claims claims) {
            return claims.userId();
        }
        if (principal instanceof CustomOAuth2User oauth2User) {
            return oauth2User.getUserId();
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}