package com.itmal.auth.service;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.internet.MimeMessage;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailVerificationServiceTest {

    private EmailVerificationStore store;
    private JavaMailSender mailSender;
    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        store = mock(EmailVerificationStore.class);
        mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        service = new EmailVerificationService(mailSender, store);
        ReflectionTestUtils.setField(service, "fromEmail", "test@gmail.com");
    }

    @Test
    void sendCode_저장후이메일발송() {
        // Arrange
        String email = "user@test.com";

        // Act
        service.sendCode(email);

        // Assert
        verify(store).save(eq(email), anyString(), eq(Duration.ofMinutes(3)));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void verifyCode_올바른코드_세션에이메일저장() {
        // Arrange
        String email = "user@test.com";
        String code = "123456";
        HttpSession session = mock(HttpSession.class);
        when(store.verify(email, code)).thenReturn(true);

        // Act
        service.verifyCode(email, code, session);

        // Assert
        verify(store).delete(email);
        verify(session).setAttribute(EmailVerificationService.SESSION_KEY, email);
    }

    @Test
    void verifyCode_잘못된코드_예외발생() {
        // Arrange
        String email = "user@test.com";
        String code = "000000";
        HttpSession session = mock(HttpSession.class);
        when(store.verify(email, code)).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.verifyCode(email, code, session));
        verify(store, never()).delete(any());
        verify(session, never()).setAttribute(any(), any());
    }

    @Test
    void verifyCode_만료된코드_예외발생() {
        // Arrange
        String email = "user@test.com";
        String code = "999999";
        HttpSession session = mock(HttpSession.class);
        when(store.verify(email, code)).thenReturn(false); // 만료 시 store가 false 반환

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.verifyCode(email, code, session));
    }
}
