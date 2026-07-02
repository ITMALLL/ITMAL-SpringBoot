package com.itmal.global.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/error")
public class ErrorPageController {

    @GetMapping("/403")
    public String forbidden(HttpServletRequest request, Model model) {
        model.addAttribute("status", request.getAttribute("status") != null ? request.getAttribute("status") : 403);
        model.addAttribute("message", request.getAttribute("message") != null ? request.getAttribute("message") : "접근 권한이 없습니다.");
        return "error/error";
    }
}
