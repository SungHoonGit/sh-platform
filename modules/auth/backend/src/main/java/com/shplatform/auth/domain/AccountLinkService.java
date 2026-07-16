package com.shplatform.auth.domain;

import com.shplatform.auth.infrastructure.UserProviderEntity;
import com.shplatform.auth.infrastructure.UserProviderRepository;
import com.shplatform.auth.infrastructure.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountLinkService {

    private static final Logger log = LoggerFactory.getLogger(AccountLinkService.class);

    private final UserRepository userRepository;
    private final UserProviderRepository userProviderRepository;

    public AccountLinkService(UserRepository userRepository,
                               UserProviderRepository userProviderRepository) {
        this.userRepository = userRepository;
        this.userProviderRepository = userProviderRepository;
    }

    public boolean isAlreadyLinked(Long userId, String provider) {
        return userProviderRepository.existsByUserIdAndProvider(userId, provider);
    }

    public List<String> getLinkedProviders(Long userId) {
        return userProviderRepository.findByUserId(userId).stream()
                .map(UserProviderEntity::getProvider)
                .toList();
    }

    @Transactional
    public void linkProvider(Long userId, String provider, String providerId, String providerEmail) {
        if (userProviderRepository.existsByUserIdAndProvider(userId, provider)) {
            log.warn("[ACCOUNT_LINK] already linked: userId={}, provider={}", userId, provider);
            return;
        }

        var providerEntity = new UserProviderEntity();
        providerEntity.setUserId(userId);
        providerEntity.setProvider(provider);
        providerEntity.setProviderId(providerId);
        providerEntity.setProviderEmail(providerEmail);
        userProviderRepository.save(providerEntity);

        log.info("[ACCOUNT_LINK] provider linked: userId={}, provider={}", userId, provider);
    }

    @Transactional
    public void unlinkProvider(Long userId, String provider) {
        long count = userProviderRepository.findByUserId(userId).size();
        if (count <= 1) {
            throw new IllegalStateException("마지막 프로바이더는 연결 해제할 수 없습니다.");
        }

        userProviderRepository.deleteByUserIdAndProvider(userId, provider);
        log.info("[ACCOUNT_LINK] provider unlinked: userId={}, provider={}", userId, provider);
    }
}
