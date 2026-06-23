package com.itmal.global.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String handleQuestionNotFound(
            BusinessException e,
            Model model) {

        model.addAttribute("message", e.getMessage());

        return "error/404";
    }
}