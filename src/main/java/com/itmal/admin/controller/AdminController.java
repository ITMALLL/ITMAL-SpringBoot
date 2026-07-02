package com.itmal.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping("/admin/reports")
    public String reportDashboard(Model model) {
        model.addAttribute("page", "admin");
        return "admin/main";
    }
}
