package com.itmal.auth.service;

import jakarta.mail.internet.InternetAddress;
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
    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    private final JavaMailSender mailSender;
    private final EmailVerificationStore store;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendCode(String email) {
        String code = generateCode();
        store.save(email, code, CODE_TTL);
        try {
            sendEmail(email, code);
        } catch (Exception e) {
            store.delete(email);
            throw e;
        }
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
            helper.setFrom(new InternetAddress(fromEmail, "잇말 - ITMAL"));
            helper.setTo(to);
            helper.setSubject("[잇말] 이메일 인증코드");
            helper.setText("""
                    안녕하세요, 잇말(ITMAL)입니다.

                    언어 학습 커뮤니티 잇말에 가입해주셔서 감사합니다.
                    아래 인증코드를 입력해 이메일 인증을 완료해주세요.

                    [인증코드] %s

                    인증코드는 발급 후 5분간 유효합니다.
                    본인이 요청하지 않은 경우 이 메일을 무시해주세요.

                    감사합니다.
                    잇말(ITMAL) 팀 드림
                    """.formatted(code));
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }
}
