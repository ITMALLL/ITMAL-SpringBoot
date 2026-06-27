package com.itmal.auth.controller;

import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.auth.dto.SocialRegisterRequest;
import com.itmal.auth.exception.DuplicateNicknameException;
import com.itmal.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/register/social")
@RequiredArgsConstructor
public class SocialRegisterController {

    private final UserService userService;

    @GetMapping
    public String form(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails.getNativeLanguage() != null) {
            return "redirect:/";
        }

        SocialRegisterRequest request = new SocialRegisterRequest();
        request.setNickname(userDetails.getNickname());
        model.addAttribute("socialRegisterRequest", request);
        addLanguagesToModel(model);
        return "user/register-social";
    }

    @PostMapping
    public String submit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute SocialRegisterRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            addLanguagesToModel(model);
            return "user/register-social";
        }

        try {
            userService.completeSocialProfile(userDetails.getUserId(), request);
        } catch (DuplicateNicknameException e) {
            addLanguagesToModel(model);
            model.addAttribute("errorMessage", "이미 사용 중인 닉네임입니다.");
            return "user/register-social";
        }

        return "redirect:/";
    }

    private void addLanguagesToModel(Model model) {
        model.addAttribute("allLanguages", userService.getAllLanguages());
    }
}
