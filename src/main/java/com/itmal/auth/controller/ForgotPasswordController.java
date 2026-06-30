package com.itmal.auth.controller;

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
            redirectAttributes.addFlashAttribute("successMessage", "임시 비밀번호가 이메일로 발송되었습니다.");
            return "redirect:/login";
        } catch (ApiException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getErrorCode().getMessage());
            return "redirect:/forgot-password";
        }
    }
}
