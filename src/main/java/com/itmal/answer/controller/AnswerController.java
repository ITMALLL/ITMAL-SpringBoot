package com.itmal.answer.controller;

import com.itmal.answer.service.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    // 답변 목록 조회
    @GetMapping
    public String getAnswers(@RequestParam Long questionId, Model model) {
        return "answers/list";
    }

    // 답변 작성
    @PostMapping
    public String createAnswer() {
        return "redirect:/answers";
    }

    // 수정 페이지
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return "answers/edit";
    }

    // 수정 처리
    @PostMapping("/{id}/edit")
    public String editAnswer(@PathVariable Long id) {
        return "redirect:/answers";
    }

    // 삭제
    @PostMapping("/{id}/delete")
    public String deleteAnswer(@PathVariable Long id) {
        return "redirect:/answers";
    }

    // 좋아요
    @PostMapping("/{id}/like")
    public String likeAnswer(@PathVariable Long id) {
        return "redirect:/answers";
    }

    // 채택
    @PostMapping("/{id}/adopt")
    public String adoptAnswer(@PathVariable Long id) {
        return "redirect:/answers";
    }

    // 신고
    @PostMapping("/{id}/report")
    public String reportAnswer(@PathVariable Long id) {
        return "redirect:/answers";
    }
}
