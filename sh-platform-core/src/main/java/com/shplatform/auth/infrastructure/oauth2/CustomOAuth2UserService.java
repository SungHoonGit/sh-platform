package com.shplatform.auth.infrastructure.oauth2;

import com.shplatform.auth.domain.UserRole;
import com.shplatform.auth.infrastructure.UserEntity;
import com.shplatform.auth.infrastructure.UserProviderEntity;
import com.shplatform.auth.infrastructure.UserProviderRepository;
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
    private final UserProviderRepository userProviderRepository;

    public CustomOAuth2UserService(UserRepository userRepository,
                                    UserProviderRepository userProviderRepository) {
        this.userRepository = userRepository;
        this.userProviderRepository = userProviderRepository;
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

        UserEntity user = findOrCreateUser(email, userInfo, provider);

        log.info("[OAUTH2] login success: provider={}, email={}, userId={}", provider, email, user.getId());

        return new CustomOAuth2User(userInfo, user.getId(), email,
                user.getRole().name(), oauth2User.getAttributes());
    }

    private UserEntity findOrCreateUser(String email, OAuth2UserInfo userInfo, String provider) {
        String providerId = userInfo.getProviderId();

        // 1. 이미 같은 프로바이더로 연결된 사용자가 있는지 확인
        var existingProvider = userProviderRepository.findByProviderAndProviderId(provider, providerId);
        if (existingProvider.isPresent()) {
            var user = userRepository.findById(existingProvider.get().getUserId()).orElseThrow();
            log.info("[OAUTH2] existing provider login: provider={}, userId={}", provider, user.getId());
            return user;
        }

        // 2. 이메일로 기존 사용자가 있는지 확인 (계정 연결)
        var existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            var user = existingUser.get();
            linkProvider(user, provider, providerId, email);
            return user;
        }

        // 3. 새 사용자 생성
        return createNewUser(email, userInfo, provider);
    }

    private void linkProvider(UserEntity user, String provider, String providerId, String email) {
        if (!userProviderRepository.existsByUserIdAndProvider(user.getId(), provider)) {
            var providerEntity = new UserProviderEntity();
            providerEntity.setUserId(user.getId());
            providerEntity.setProvider(provider);
            providerEntity.setProviderId(providerId);
            providerEntity.setProviderEmail(email);
            userProviderRepository.save(providerEntity);
            log.info("[OAUTH2] provider linked: userId={}, provider={}", user.getId(), provider);
        }

        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }
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

        var providerEntity = new UserProviderEntity();
        providerEntity.setUserId(saved.getId());
        providerEntity.setProvider(provider);
        providerEntity.setProviderId(userInfo.getProviderId());
        providerEntity.setProviderEmail(email);
        userProviderRepository.save(providerEntity);

        log.info("[OAUTH2] new user created: id={}, email={}, provider={}", saved.getId(), email, provider);
        return saved;
    }
}
