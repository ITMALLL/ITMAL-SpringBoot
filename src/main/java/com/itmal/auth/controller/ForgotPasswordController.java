package com.itmal.auth.controller;

import com.itmal.auth.exception.EmailSendException;
import com.itmal.auth.service.UserService;
import com.itmal.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final UserService userService;

    @GetMapping("/forgot-password")
    public String form() {
        return "user/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String submit(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            userService.resetPassword(email);
        } catch (ApiException | EmailSendException e) {
            log.info("[forgot-password] 처리 중 오류 발생 - {}", e.getMessage());
        }
        redirectAttributes.addFlashAttribute("successMessage", "입력하신 이메일로 안내를 보냈습니다. 가입 방식에 따라 메일이 발송되지 않을 수 있습니다.");
        return "redirect:/login";
    }
}
