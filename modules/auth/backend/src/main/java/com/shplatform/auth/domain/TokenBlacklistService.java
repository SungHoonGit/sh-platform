package com.shplatform.auth.domain;

import com.shplatform.shared.config.RedisRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final RedisRepository redisRepository;

    public TokenBlacklistService(RedisRepository redisRepository) {
        this.redisRepository = redisRepository;
    }

    /**
     * Access Token을 블랙리스트에 추가한다.
     *
     * @param token 블랙리스트에 추가할 JWT
     * @param remainingSeconds 토큰 남은 유효시간 (초)
     */
    public void addToBlacklist(String token, long remainingSeconds) {
        String hash = hashToken(token);
        String key = BLACKLIST_PREFIX + hash;
        redisRepository.set(key, "1", remainingSeconds, TimeUnit.SECONDS);
        log.info("[BLACKLIST] token added, expires in {}s", remainingSeconds);
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인한다.
     *
     * @param token 확인할 JWT
     * @return 블랙리스트에 있으면 true
     */
    public boolean isBlacklisted(String token) {
        String hash = hashToken(token);
        String key = BLACKLIST_PREFIX + hash;
        return Boolean.TRUE.equals(redisRepository.hasKey(key));
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
