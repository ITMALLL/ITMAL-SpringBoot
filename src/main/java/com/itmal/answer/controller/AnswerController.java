package com.itmal.answer.controller;

import com.itmal.answer.dto.AnswerCreateRequest;
import com.itmal.answer.dto.AnswerUpdateRequest;
import com.itmal.answer.service.AnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
    public String createAnswer(@Valid @ModelAttribute AnswerCreateRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "answers/write";
        }
        return "redirect:/answers?questionId=" + request.getQuestionId();
    }

    // 수정 페이지
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return "answers/edit";
    }

    // 수정 처리
    @PostMapping("/{id}/edit")
    public String editAnswer(@PathVariable Long id, @RequestParam Long questionId,
                             @Valid @ModelAttribute AnswerUpdateRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "answers/edit";
        }
        return "redirect:/answers?questionId=" + questionId;
    }

    // 삭제
    @PostMapping("/{id}/delete")
    public String deleteAnswer(@PathVariable Long id, @RequestParam Long questionId) {
        return "redirect:/answers?questionId=" + questionId;
    }

    // 좋아요
    @PostMapping("/{id}/like")
    public String likeAnswer(@PathVariable Long id, @RequestParam Long questionId) {
        return "redirect:/answers?questionId=" + questionId;
    }

    // 채택
    @PostMapping("/{id}/adopt")
    public String adoptAnswer(@PathVariable Long id, @RequestParam Long questionId) {
        return "redirect:/answers?questionId=" + questionId;
    }

    // 신고
    @PostMapping("/{id}/report")
    public String reportAnswer(@PathVariable Long id, @RequestParam Long questionId) {
        return "redirect:/answers?questionId=" + questionId;
    }
}
