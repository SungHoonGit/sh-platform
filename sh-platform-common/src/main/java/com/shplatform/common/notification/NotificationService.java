package com.shplatform.common.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationConfigRepository configRepository;
    private final NotificationLogRepository logRepository;

    @Transactional(readOnly = true)
    public List<NotificationConfig> getConfigsByModule(String moduleName) {
        return configRepository.findByModuleNameAndIsEnabledTrue(moduleName);
    }

    @Transactional
    public NotificationConfig createConfig(NotificationConfig config) {
        return configRepository.save(config);
    }

    @Transactional
    public NotificationConfig updateConfig(Long id, NotificationConfig updated) {
        NotificationConfig config = configRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Notification config not found: " + id));
        
        config.setEventType(updated.getEventType());
        config.setNotificationType(updated.getNotificationType());
        config.setIsEnabled(updated.getIsEnabled());
        config.setRecipientEmail(updated.getRecipientEmail());
        config.setRecipientPhone(updated.getRecipientPhone());
        
        return configRepository.save(config);
    }

    @Transactional
    public void deleteConfig(Long id) {
        configRepository.deleteById(id);
    }

    @Transactional
    public void sendNotification(String moduleName, String eventType, String content) {
        List<NotificationConfig> configs = configRepository
            .findByModuleNameAndIsEnabledTrue(moduleName);
        
        for (NotificationConfig config : configs) {
            if (config.getEventType().equals(eventType)) {
                sendSingleNotification(config, content);
            }
        }
    }

    private void sendSingleNotification(NotificationConfig config, String content) {
        NotificationLog notificationLog = NotificationLog.builder()
            .notificationConfig(config)
            .moduleName(config.getModuleName())
            .eventType(config.getEventType())
            .recipient(getRecipient(config))
            .content(content)
            .status(NotificationLog.NotificationStatus.PENDING)
            .build();
        
        try {
            switch (config.getNotificationType()) {
                case EMAIL -> sendEmail(config.getRecipientEmail(), content);
                case KAKAO -> sendKakao(config.getRecipientPhone(), content);
                case WEBPUSH -> sendWebPush(content);
            }
            
            notificationLog.setStatus(NotificationLog.NotificationStatus.SENT);
            notificationLog.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage());
            notificationLog.setStatus(NotificationLog.NotificationStatus.FAILED);
            notificationLog.setErrorMessage(e.getMessage());
        }
        
        logRepository.save(notificationLog);
    }

    private String getRecipient(NotificationConfig config) {
        return switch (config.getNotificationType()) {
            case EMAIL -> config.getRecipientEmail();
            case KAKAO -> config.getRecipientPhone();
            case WEBPUSH -> "webpush";
        };
    }

    private void sendEmail(String to, String content) {
        log.info("Sending email to {}: {}", to, content);
    }

    private void sendKakao(String phone, String content) {
        log.info("Sending Kakao to {}: {}", phone, content);
    }

    private void sendWebPush(String content) {
        log.info("Sending web push: {}", content);
    }

    @Transactional(readOnly = true)
    public List<NotificationLog> getRecentLogs(String moduleName) {
        return logRepository.findTop10ByModuleNameOrderByCreatedAtDesc(moduleName);
    }
}
