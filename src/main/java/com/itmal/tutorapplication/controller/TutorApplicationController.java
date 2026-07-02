package com.itmal.tutorapplication.controller;

import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.auth.domain.Role;
import com.itmal.auth.repository.UserMapper;
import com.itmal.global.exception.BusinessException;
import com.itmal.global.exception.ErrorCode;
import com.itmal.tutorapplication.service.TutorApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class TutorApplicationController {

    private final TutorApplicationService tutorApplicationService;
    private final UserMapper userMapper;

    @PostMapping("/tutor-applications")
    public String apply(@AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            tutorApplicationService.apply(userDetails.getUserId());
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.ALREADY_APPLIED) {
                return "redirect:/mypage";
            }
            if (e.getErrorCode() == ErrorCode.ALREADY_TUTOR) {
                return "redirect:/mypage";
            }
            throw e;
        }
        return "redirect:/mypage";
    }

    // 튜터 신청 목록
    @GetMapping("/admin/tutor-applications")
    public String getPendingApplications(Model model) {
        model.addAttribute("applications", tutorApplicationService.getPendingApplicationsWithUser());
        return "admin/tutor-applications";
    }

    @PostMapping("/admin/tutor-applications/{id}/approve")
    public String approve(@PathVariable Long id) {
        tutorApplicationService.approve(id);
        return "redirect:/admin/tutor-applications";
    }

    @PostMapping("/admin/tutor-applications/{id}/reject")
    public String reject(@PathVariable Long id) {
        tutorApplicationService.reject(id);
        return "redirect:/admin/tutor-applications";
    }

    // 튜터 목록 관리
    @GetMapping("/admin/tutors")
    public String getTutors(Model model) {
        model.addAttribute("tutors", userMapper.findAllTutors());
        return "admin/tutors";
    }

    // 튜터 박탈
    @PostMapping("/admin/tutors/{userId}/revoke")
    public String revokeTutor(@PathVariable Long userId) {
        com.itmal.auth.domain.User user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getRole() != Role.ROLE_TUTOR) {
            throw new BusinessException(ErrorCode.NOT_A_TUTOR);
        }
        userMapper.updateRole(userId, Role.ROLE_USER.name());
        return "redirect:/admin/tutors";
    }
}
