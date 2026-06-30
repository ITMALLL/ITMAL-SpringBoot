package com.itmal.notification.controller;

import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class NotificationViewController {

    private final NotificationService notificationService;

    @GetMapping("/notifications")
    public String notificationPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            model.addAttribute("notifications", notificationService.getNotifications(userDetails.getUserId()));
        }
        model.addAttribute("page", "notifications");
        return "user/noti-list";
    }
}
