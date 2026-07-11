package com.shplatform.auth.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationCode(String to, String code) {
        var message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[SH Platform] 이메일 인증 코드");
        message.setText("인증 코드: " + code + "\n\n이 코드는 5분 후 만료됩니다.");
        try {
            mailSender.send(message);
            log.info("Verification email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
        }
    }
}
