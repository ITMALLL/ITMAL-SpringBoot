package com.itmal.auth.service;

import com.itmal.auth.exception.EmailSendException;
import com.itmal.auth.exception.TooManyRequestsException;
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
        verify(store).reserveSend(email);
        verify(mailSender).send(any(MimeMessage.class));
        verify(store).save(eq(email), anyString(), eq(Duration.ofMinutes(5)));
    }

    @Test
    void sendCode_쿨다운중_예외발생() {
        // Arrange
        String email = "user@test.com";
        doThrow(new TooManyRequestsException("60초에 한 번만 가능합니다."))
                .when(store).reserveSend(email);

        // Act & Assert
        assertThrows(TooManyRequestsException.class, () -> service.sendCode(email));
        verify(mailSender, never()).send(any(MimeMessage.class));
        verify(store, never()).save(any(), any(), any());
    }

    @Test
    void sendCode_발송실패시_예약롤백() {
        // Arrange
        String email = "user@test.com";
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP 연결 실패"));

        // Act & Assert
        assertThrows(EmailSendException.class, () -> service.sendCode(email));
        verify(store, never()).save(any(), any(), any());
        verify(store).releaseReservation(email);
    }

    @Test
    void verifyCode_올바른코드_검증성공() {
        // Arrange
        String email = "user@test.com";
        String code = "123456";
        when(store.verify(email, code)).thenReturn(true);

        // Act
        service.verifyCode(email, code);

        // Assert
        verify(store).verify(email, code);
        verify(store, never()).delete(any());
    }

    @Test
    void verifyCode_잘못된코드_예외발생() {
        // Arrange
        String email = "user@test.com";
        String code = "000000";
        when(store.verify(email, code)).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.verifyCode(email, code));
        verify(store, never()).delete(any());
    }
}
