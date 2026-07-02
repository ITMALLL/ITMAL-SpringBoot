package com.itmal.admin.controller;

import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.global.exception.ErrorCode;
import com.itmal.global.exception.ViewException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping("/admin/reports")
    public String reportDashboard(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))){
            throw new ViewException(ErrorCode.FORBIDDEN);
        }
        model.addAttribute("page", "admin");
        return "admin/main";
    }
}
