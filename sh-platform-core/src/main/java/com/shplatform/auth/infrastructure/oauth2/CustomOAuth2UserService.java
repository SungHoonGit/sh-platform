package com.shplatform.auth.infrastructure.oauth2;

import com.shplatform.auth.domain.UserRole;
import com.shplatform.auth.infrastructure.UserEntity;
import com.shplatform.auth.infrastructure.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.create(provider, oauth2User.getAttributes());

        String email = userInfo.getEmail();
        if (email == null || email.isBlank()) {
            log.warn("[OAUTH2] No email from provider={}, providerId={}", provider, userInfo.getProviderId());
            throw new OAuth2AuthenticationException("이메일 정보를 가져올 수 없습니다.");
        }

        UserEntity user = userRepository.findByEmail(email)
                .map(existing -> updateProvider(existing, provider, userInfo))
                .orElseGet(() -> createNewUser(email, userInfo, provider));

        log.info("[OAUTH2] login success: provider={}, email={}, userId={}", provider, email, user.getId());

        return new CustomOAuth2User(userInfo, user.getId(), email,
                user.getRole().name(), oauth2User.getAttributes());
    }

    private UserEntity updateProvider(UserEntity existing, String provider, OAuth2UserInfo userInfo) {
        boolean changed = false;
        if (existing.getProvider().equals("LOCAL")) {
            existing.setProvider(provider);
            existing.setProviderId(userInfo.getProviderId());
            changed = true;
        }
        if (!existing.isEmailVerified()) {
            existing.setEmailVerified(true);
            changed = true;
        }
        if (changed) {
            userRepository.save(existing);
            log.info("[OAUTH2] updated user: id={}, provider={}", existing.getId(), provider);
        }
        return existing;
    }

    private UserEntity createNewUser(String email, OAuth2UserInfo userInfo, String provider) {
        var entity = new UserEntity();
        entity.setEmail(email);
        entity.setPassword(null);
        entity.setName(userInfo.getName() != null ? userInfo.getName() : email);
        entity.setRole(UserRole.USER);
        entity.setProvider(provider);
        entity.setProviderId(userInfo.getProviderId());
        entity.setEmailVerified(true);
        entity.setLocale("ko");

        var saved = userRepository.save(entity);
        log.info("[OAUTH2] new user created: id={}, email={}, provider={}", saved.getId(), email, provider);
        return saved;
    }
}
