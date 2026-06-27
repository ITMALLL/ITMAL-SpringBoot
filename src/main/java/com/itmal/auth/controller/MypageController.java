package com.itmal.auth.controller;

import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.auth.dto.PasswordChangeRequest;
import com.itmal.auth.dto.ProfileUpdateRequest;
import com.itmal.auth.exception.DuplicateNicknameException;
import com.itmal.auth.service.SecuritySessionService;
import com.itmal.auth.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final UserService userService;
    private final SecuritySessionService securitySessionService;

    @GetMapping
    public String mypage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("user", userDetails);
        if (!model.containsAttribute("profileUpdateRequest")) {
            loadProfileModel(userDetails, model);
        } else {
            model.addAttribute("allLanguages", userService.getAllLanguages());
        }
        return "user/mypage";
    }

    @PostMapping("/edit")
    public String editProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute ProfileUpdateRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("profileUpdateRequest", request);
            redirectAttributes.addFlashAttribute("editError", "입력값을 확인해주세요.");
            redirectAttributes.addFlashAttribute("openEditModal", true);
            return "redirect:/mypage";
        }

        try {
            userService.updateProfile(userDetails.getUserId(), request);
        } catch (DuplicateNicknameException e) {
            redirectAttributes.addFlashAttribute("editError", "이미 사용 중인 닉네임입니다.");
            redirectAttributes.addFlashAttribute("openEditModal", true);
            return "redirect:/mypage";
        }

        securitySessionService.refreshSession(userDetails.getUsername(), httpRequest);
        redirectAttributes.addFlashAttribute("successMessage", "프로필이 수정되었습니다.");
        return "redirect:/mypage";
    }

    @GetMapping("/password")
    public String passwordForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("user", userDetails);
        model.addAttribute("passwordChangeRequest", new PasswordChangeRequest());
        return "user/mypage-password";
    }

    @PostMapping("/password")
    public String changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute PasswordChangeRequest request,
            BindingResult bindingResult,
            Model model,
            HttpServletRequest httpRequest,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userDetails);
            return "user/mypage-password";
        }

        try {
            userService.changePassword(userDetails.getUserId(), request);
        } catch (IllegalStateException | IllegalArgumentException e) {
            model.addAttribute("user", userDetails);
            model.addAttribute("errorMessage", e.getMessage());
            return "user/mypage-password";
        }

        securitySessionService.refreshSession(userDetails.getUsername(), httpRequest);
        redirectAttributes.addFlashAttribute("successMessage", "비밀번호가 변경되었습니다.");
        return "redirect:/mypage";
    }

    @PostMapping("/delete")
    public String deleteAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request
    ) {
        userService.deleteAccount(userDetails.getUserId());
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login?deleted";
    }

    private void loadProfileModel(CustomUserDetails userDetails, Model model) {
        ProfileUpdateRequest profileRequest = new ProfileUpdateRequest();
        profileRequest.setNickname(userDetails.getNickname());
        profileRequest.setNativeLanguage(userDetails.getNativeLanguage());
        profileRequest.setLearningLanguages(userService.getLearningLanguages(userDetails.getUserId()));
        model.addAttribute("profileUpdateRequest", profileRequest);
        model.addAttribute("allLanguages", userService.getAllLanguages());
    }


}
