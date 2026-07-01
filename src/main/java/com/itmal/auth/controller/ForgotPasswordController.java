package com.itmal.auth.controller;

import com.itmal.auth.exception.EmailSendException;
import com.itmal.auth.service.UserService;
import com.itmal.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final UserService userService;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @GetMapping("/forgot-password")
    public String form() {
        return "user/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String submit(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            String resetBaseUrl = UriComponentsBuilder.fromUriString(appBaseUrl)
                    .path("/forgot-password/reset")
                    .build().toUriString();
            userService.initiatePasswordReset(email, resetBaseUrl);
        } catch (EmailSendException e) {
            log.info("[forgot-password] 이메일 발송 실패 - {}", e.getMessage());
        }
        redirectAttributes.addFlashAttribute("successMessage", "입력하신 이메일로 안내를 보냈습니다. 가입 방식에 따라 메일이 발송되지 않을 수 있습니다.");
        return "redirect:/login";
    }

    @GetMapping("/forgot-password/reset")
    public String resetForm(@RequestParam String token, Model model) {
        if (!userService.isValidResetToken(token)) {
            return "redirect:/forgot-password?expired";
        }
        model.addAttribute("token", token);
        return "user/reset-password";
    }

    @PostMapping("/forgot-password/reset")
    public String resetSubmit(@RequestParam String token,
                              @RequestParam String newPassword,
                              @RequestParam String confirmPassword,
                              RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "비밀번호가 일치하지 않습니다.");
            return "redirect:/forgot-password/reset?token=" + token;
        }
        try {
            userService.confirmPasswordReset(token, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요.");
            return "redirect:/login";
        } catch (ApiException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "링크가 만료되었거나 유효하지 않습니다.");
            return "redirect:/forgot-password";
        }
    }
}
