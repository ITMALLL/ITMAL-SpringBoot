package com.itmal.auth.service;

import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    public static final String SESSION_KEY = "VERIFIED_EMAIL";
    private static final Duration CODE_TTL = Duration.ofMinutes(3);

    private final JavaMailSender mailSender;
    private final EmailVerificationStore store;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendCode(String email) {
        String code = generateCode();
        store.save(email, code, CODE_TTL);
        sendEmail(email, code);
    }

    public void verifyCode(String email, String code, HttpSession session) {
        if (!store.verify(email, code)) {
            throw new IllegalArgumentException("인증코드가 올바르지 않거나 만료되었습니다.");
        }
        store.delete(email);
        session.setAttribute(SESSION_KEY, email);
    }

    private String generateCode() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    private void sendEmail(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("[잇말] 이메일 인증코드");
            helper.setText("""
                    안녕하세요! 잇말입니다.

                    이메일 인증코드: %s

                    3분 이내에 입력해주세요.
                    """.formatted(code));
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }
}
