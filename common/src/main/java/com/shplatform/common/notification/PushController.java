package com.shplatform.common.notification;

import com.shplatform.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/push")
@RequiredArgsConstructor
@Tag(name = "PushNotification", description = "웹 푸쉬 구독 관리 API")
public class PushController {

    private final WebPushService webPushService;

    @PostMapping("/subscribe")
    @Operation(summary = "푸쉬 구독", description = "브라우저 푸쉬 구독을 저장합니다")
    public ResponseEntity<Map<String, String>> subscribe(@RequestBody Map<String, String> body) {
        Long accountId = SecurityUtils.currentAccountId();
        String endpoint = body.get("endpoint");
        String p256dh = body.get("p256dh");
        String authKey = body.get("auth");
        String userAgent = body.get("userAgent");

        webPushService.subscribe(accountId, endpoint, p256dh, authKey, userAgent);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @DeleteMapping("/unsubscribe")
    @Operation(summary = "푸쉬 구독 해제", description = "브라우저 푸쉬 구독을 해제합니다")
    public ResponseEntity<Map<String, String>> unsubscribe(@RequestBody Map<String, String> body) {
        String endpoint = body.get("endpoint");
        webPushService.unsubscribe(endpoint);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/vapid-public-key")
    @Operation(summary = "VAPID 공개키 조회", description = "프론트엔드에서 푸쉬 구독에 사용할 VAPID 공개키를 반환합니다")
    public ResponseEntity<Map<String, String>> getVapidPublicKey(@org.springframework.beans.factory.annotation.Value("${webpush.vapid.public-key:}") String publicKey) {
        return ResponseEntity.ok(Map.of("publicKey", publicKey));
    }
}
