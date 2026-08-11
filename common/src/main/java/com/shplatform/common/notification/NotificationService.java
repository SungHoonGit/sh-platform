package com.shplatform.common.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
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
    private final JavaMailSender mailSender;

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

    @Async
    public void sendNotificationAsync(String moduleName, String eventType, String subject, String content) {
        sendNotification(moduleName, eventType, subject + "\n\n" + content);
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
        try {
            // 첫 줄에서 제목 추출
            String[] lines = content.split("\n");
            String subject = "[SH Platform] 알림";
            String body = content;
            
            if (lines.length > 0 && lines[0].contains("Config:")) {
                subject = "[SH Platform] 신규 채용공고 수집 완료";
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("noreply@shplatform.com");
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw e;
        }
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
