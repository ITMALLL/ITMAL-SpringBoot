package com.itmal.mypage.controller;

import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class UserProfileController {
    //다른 유저가 나의 프로필을 볼 때 controller

    private final UserService userService;

    @GetMapping("/users/{userId}")
    public String getUserId(@PathVariable Long userId,
                          @AuthenticationPrincipal CustomUserDetails userDetails,
                          Model model){
        model.addAttribute("user", userService.findById(userId));
        model.addAttribute("isMyPage", userDetails != null && userDetails.getUserId().equals(userId));
        return "user/mypage";
    }
}
