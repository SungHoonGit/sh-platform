package com.shplatform.common.notification.controller;

import com.shplatform.common.notification.NotificationConfig;
import com.shplatform.common.notification.NotificationLog;
import com.shplatform.common.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Notification", description = "알림 관리 API")
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 설정 목록 조회")
    @GetMapping("/configs")
    public ResponseEntity<List<NotificationConfig>> getConfigs(
            @RequestParam(value = "moduleName", required = false) String moduleName) {
        if (moduleName != null) {
            return ResponseEntity.ok(notificationService.getConfigsByModule(moduleName));
        }
        return ResponseEntity.ok(notificationService.getConfigsByModule("scraper"));
    }

    @Operation(summary = "알림 설정 생성")
    @PostMapping("/configs")
    public ResponseEntity<NotificationConfig> createConfig(@RequestBody NotificationConfig config) {
        return ResponseEntity.ok(notificationService.createConfig(config));
    }

    @Operation(summary = "알림 설정 수정")
    @PutMapping("/configs/{id}")
    public ResponseEntity<NotificationConfig> updateConfig(
            @PathVariable(value = "id") Long id,
            @RequestBody NotificationConfig config) {
        return ResponseEntity.ok(notificationService.updateConfig(id, config));
    }

    @Operation(summary = "알림 설정 삭제")
    @DeleteMapping("/configs/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable(value = "id") Long id) {
        notificationService.deleteConfig(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "알림 전송")
    @PostMapping("/send")
    public ResponseEntity<Void> sendNotification(
            @RequestParam(value = "moduleName") String moduleName,
            @RequestParam(value = "eventType") String eventType,
            @RequestBody String content) {
        notificationService.sendNotification(moduleName, eventType, content);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "알림 이력 조회")
    @GetMapping("/logs")
    public ResponseEntity<List<NotificationLog>> getLogs(
            @RequestParam(value = "moduleName", required = false) String moduleName) {
        String module = moduleName != null ? moduleName : "scraper";
        return ResponseEntity.ok(notificationService.getRecentLogs(module));
    }
}
