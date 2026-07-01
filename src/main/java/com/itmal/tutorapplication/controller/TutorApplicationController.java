package com.itmal.tutorapplication.controller;

import com.itmal.auth.domain.CustomUserDetails;
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

    @PostMapping("/tutor-applications")
    public String apply(@AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            tutorApplicationService.apply(userDetails.getUserId());
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.ALREADY_APPLIED) {
                return "redirect:/mypage?error=already_applied";
            }
            if (e.getErrorCode() == ErrorCode.ALREADY_TUTOR) {
                return "redirect:/mypage?error=already_tutor";
            }
            throw e;
        }
        return "redirect:/mypage";
    }

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
}
