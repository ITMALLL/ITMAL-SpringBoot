package com.itmal.auth.controller;

import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.auth.domain.User;
import com.itmal.auth.dto.PasswordChangeRequest;
import com.itmal.auth.dto.ProfileUpdateRequest;
import com.itmal.auth.exception.DuplicateNicknameException;
import com.itmal.auth.service.CustomUserDetailsService;
import com.itmal.auth.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final UserService userService;
    private final CustomUserDetailsService customUserDetailsService;

    @GetMapping
    public String mypage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("user", userDetails);
        loadProfileModel(userDetails.getUserId(), model);
        return "user/mypage";
    }

    @GetMapping("/edit")
    public String editForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("user", userDetails);
        loadProfileModel(userDetails.getUserId(), model);
        return "user/mypage-edit";
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

        refreshSession(userDetails.getUsername(), httpRequest);
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

    private void loadProfileModel(Long userId, Model model) {
        User user = userService.findById(userId);
        ProfileUpdateRequest profileRequest = new ProfileUpdateRequest();
        profileRequest.setNickname(user.getNickname());
        profileRequest.setNativeLanguage(user.getNativeLanguage());
        profileRequest.setLearningLanguages(userService.getLearningLanguages(userId));
        model.addAttribute("profileUpdateRequest", profileRequest);
        model.addAttribute("allLanguages", userService.getAllLanguages());
    }

    private void refreshSession(String email, HttpServletRequest request) {
        UserDetails updated = customUserDetailsService.loadUserByUsername(email);
        Authentication newAuth = UsernamePasswordAuthenticationToken.authenticated(
                updated, null, updated.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(newAuth);
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());
        }
    }
}
