package com.itmal.auth.controller;

import com.itmal.auth.dto.RegisterRequest;
import com.itmal.auth.exception.DuplicateEmailException;
import com.itmal.auth.exception.DuplicateNicknameException;
import com.itmal.auth.service.AuthService;
import com.itmal.auth.service.EmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String loginForm() {
        return "user/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "user/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute RegisterRequest request,
            BindingResult bindingResult,
            Model model,
            HttpServletRequest httpRequest
    ) {
        HttpSession session = httpRequest.getSession(false);
        String verifiedEmail = session != null ? (String) session.getAttribute(EmailVerificationService.SESSION_KEY) : null;

        if (bindingResult.hasErrors()) {
            restoreEmailVerified(request.getEmail(), verifiedEmail, model);
            return "user/register";
        }

        if (!request.getEmail().equals(verifiedEmail)) {
            model.addAttribute("errorMessage", "이메일 인증이 필요합니다.");
            return "user/register";
        }

        try {
            authService.register(request);
            session.removeAttribute(EmailVerificationService.SESSION_KEY);
        } catch (DuplicateEmailException e) {
            session.removeAttribute(EmailVerificationService.SESSION_KEY);
            model.addAttribute("errorMessage", "이미 사용 중인 이메일입니다.");
            return "user/register";
        } catch (DuplicateNicknameException e) {
            restoreEmailVerified(request.getEmail(), verifiedEmail, model);
            model.addAttribute("errorMessage", "이미 사용 중인 닉네임입니다.");
            return "user/register";
        } catch (IllegalArgumentException e) {
            restoreEmailVerified(request.getEmail(), verifiedEmail, model);
            model.addAttribute("errorMessage", e.getMessage());
            return "user/register";
        }

        return "redirect:/login";
    }

    private void restoreEmailVerified(String requestEmail, String verifiedEmail, Model model) {
        if (requestEmail != null && requestEmail.equals(verifiedEmail)) {
            model.addAttribute("emailVerified", true);
        }
    }
}
