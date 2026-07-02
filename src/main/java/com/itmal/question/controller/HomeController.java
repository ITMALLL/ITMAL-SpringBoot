package com.itmal.question.controller;

import com.itmal.question.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// 홈 화면("/") — 상단 통계, 최근 질문, 언어별 현황을 채워 렌더링한다.
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final QuestionService questionService;

    @GetMapping("/")
    public String index(Model model) {
        // 1) 상단 통계 4개
        model.addAttribute("questionCount", questionService.countAllQuestions());
        model.addAttribute("answerCount", questionService.countAllAnswers());
        model.addAttribute("activeUserCount", questionService.countActiveUsers());
        model.addAttribute("languageCount", questionService.countLanguages());

        // 2) 최근 질문(최신순 5건)
        model.addAttribute("recentQuestions", questionService.findRecentQuestions());

        // 3) 언어별 질문 현황
        model.addAttribute("languageStats", questionService.findLanguageStats());

        return "index";
    }
}
