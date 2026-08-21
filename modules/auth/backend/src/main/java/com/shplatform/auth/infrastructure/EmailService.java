package com.shplatform.auth.infrastructure;

import com.shplatform.common.notification.EmailTemplate;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * (명령형) 회원가입/비밀번호 재설정용 이메일 인증 코드를 HTML 형식으로 발송한다.
     *
     * @param to   수신자 이메일
     * @param code 6자리 인증 코드
     */
    public void sendVerificationCode(String to, String code) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("[SH Platform] 이메일 인증 코드");
            helper.setFrom("noreply@shplatform.com");
            helper.setText(buildVerificationCodeHtml(code), true);
            mailSender.send(message);
            log.info("Verification email sent to: {}", to);
        } catch (MessagingException | RuntimeException e) {
            log.error("Failed to send email to: {}", to, e);
        }
    }

    private String buildVerificationCodeHtml(String code) {
        String body = "<p style=\"margin:0 0 18px;font-size:14px;color:#334155;line-height:1.7;\">안녕하세요.<br>"
                + "아래 인증 코드를 입력해 주세요.</p>"
                + EmailTemplate.codeBox(code)
                + "<p style=\"margin:0 0 10px;font-size:13px;color:#64748b;\">이 코드는 "
                + "<b style=\"color:#1e293b;\">5분</b> 후 만료됩니다.</p>"
                + "<p style=\"margin:0;font-size:12px;color:#94a3b8;\">본인이 요청하지 않은 경우 이 메일은 무시하셔도 됩니다.</p>";
        return EmailTemplate.frame("이메일 인증 코드", "SH Platform 회원 인증", body,
                "회원가입 과정의 이메일 인증 요청에 의해 발송되었습니다.");
    }
}
