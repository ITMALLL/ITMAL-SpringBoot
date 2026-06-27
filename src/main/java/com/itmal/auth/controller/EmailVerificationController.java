package com.itmal.auth.controller;

import com.itmal.auth.dto.EmailSendRequest;
import com.itmal.auth.dto.EmailVerifyRequest;
import com.itmal.auth.service.EmailVerificationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
        emailVerificationService.sendCode(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Boolean>> verify(
            @Valid @RequestBody EmailVerifyRequest request,
            HttpSession session
    ) {
        try {
            emailVerificationService.verifyCode(request.getEmail(), request.getCode(), session);
            return ResponseEntity.ok(Map.of("verified", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("verified", false));
        }
    }
}
