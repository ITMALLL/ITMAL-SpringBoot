package com.itmal.global.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ErrorPageController implements ErrorController {

    @RequestMapping("/error/403")
    public String forbidden(HttpServletRequest request, Model model) {
        model.addAttribute("status", request.getAttribute("status") != null ? request.getAttribute("status") : 403);
        model.addAttribute("message", request.getAttribute("message") != null ? request.getAttribute("message") : "접근 권한이 없습니다.");
        return "error/error";
    }

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = statusAttr != null ? Integer.parseInt(statusAttr.toString()) : HttpStatus.INTERNAL_SERVER_ERROR.value();

        model.addAttribute("status", status);
        model.addAttribute("message", defaultMessage(status));
        return "error/error";
    }

    private String defaultMessage(int status) {
        return switch (status) {
            case 404 -> "요청하신 페이지를 찾을 수 없습니다.";
            case 403 -> "접근 권한이 없습니다.";
            default -> "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        };
    }

}
