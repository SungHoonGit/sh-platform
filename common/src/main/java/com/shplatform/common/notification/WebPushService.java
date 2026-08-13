package com.shplatform.common.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Notification;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.Security;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebPushService {

    private final PushSubscriptionRepository pushSubscriptionRepository;

    @Value("${webpush.vapid.public-key:}")
    private String vapidPublicKey;

    @Value("${webpush.vapid.private-key:}")
    private String vapidPrivateKey;

    @Value("${webpush.vapid.subject:mailto:noreply@shplatform.com}")
    private String vapidSubject;

    private PushService pushService;

    @PostConstruct
    public void init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        if (vapidPublicKey != null && !vapidPublicKey.isEmpty()
                && vapidPrivateKey != null && !vapidPrivateKey.isEmpty()) {
            try {
                pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
                log.info("[PUSH] WebPush service initialized");
            } catch (Exception e) {
                log.error("[PUSH] Failed to initialize WebPush: {}", e.getMessage(), e);
            }
        } else {
            log.warn("[PUSH] VAPID keys not configured, push notifications disabled");
        }
    }

    public void sendPushToUser(Long accountId, String title, String body, String url) {
        if (pushService == null) {
            log.debug("[PUSH] PushService not initialized, skipping");
            return;
        }

        List<PushSubscription> subscriptions = pushSubscriptionRepository.findByAccountIdAndIsActiveTrue(accountId);
        if (subscriptions.isEmpty()) {
            log.debug("[PUSH] No active subscriptions for account: {}", accountId);
            return;
        }

        String payload = String.format("{\"title\":\"%s\",\"body\":\"%s\",\"url\":\"%s\"}",
                title, body, url != null ? url : "/scraper/viewer");

        int success = 0;
        int failed = 0;

        for (PushSubscription sub : subscriptions) {
            try {
                Notification notification = new Notification(
                        sub.getEndpoint(),
                        sub.getP256dh(),
                        sub.getAuthKey(),
                        payload);

                pushService.send(notification);
                success++;
            } catch (Exception e) {
                log.warn("[PUSH] Failed to send to {}: {}", sub.getEndpoint(), e.getMessage());
                sub.setIsActive(false);
                pushSubscriptionRepository.save(sub);
                failed++;
            }
        }

        log.info("[PUSH] Sent push notifications: success={}, failed={}, total={}", success, failed, subscriptions.size());
    }

    public PushSubscription subscribe(Long accountId, String endpoint, String p256dh, String authKey, String userAgent) {
        PushSubscription existing = pushSubscriptionRepository.findByEndpoint(endpoint).orElse(null);

        if (existing != null) {
            existing.setIsActive(true);
            existing.setAccountId(accountId);
            existing.setP256dh(p256dh);
            existing.setAuthKey(authKey);
            existing.setUserAgent(userAgent);
            existing.setUpdatedAt(java.time.LocalDateTime.now());
            return pushSubscriptionRepository.save(existing);
        }

        PushSubscription subscription = PushSubscription.builder()
                .accountId(accountId)
                .endpoint(endpoint)
                .p256dh(p256dh)
                .authKey(authKey)
                .userAgent(userAgent)
                .isActive(true)
                .build();
        return pushSubscriptionRepository.save(subscription);
    }

    public void unsubscribe(String endpoint) {
        pushSubscriptionRepository.deactivateByEndpoint(endpoint);
    }
}
