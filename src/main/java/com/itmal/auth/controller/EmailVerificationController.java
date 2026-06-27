package com.itmal.auth.controller;

import com.itmal.auth.dto.EmailSendRequest;
import com.itmal.auth.dto.EmailVerifyRequest;
import com.itmal.auth.exception.TooManyRequestsException;
import com.itmal.auth.service.EmailVerificationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @PostMapping("/send")
    public ResponseEntity<Void> send(@Valid @RequestBody EmailSendRequest request) {
        try {
            emailVerificationService.sendCode(request.getEmail());
            return ResponseEntity.ok().build();
        } catch (TooManyRequestsException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Boolean>> verify(
            @Valid @RequestBody EmailVerifyRequest request,
            HttpSession session
    ) {
        try {
            emailVerificationService.verifyCode(request.getEmail(), request.getCode());
            session.setAttribute(EmailVerificationService.SESSION_KEY, request.getEmail());
            return ResponseEntity.ok(Map.of("verified", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("verified", false));
        }
    }
}
