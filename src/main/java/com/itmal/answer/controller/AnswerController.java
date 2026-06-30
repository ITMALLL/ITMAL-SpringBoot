package com.itmal.answer.controller;

import com.itmal.answer.dto.AnswerCreateRequest;
import com.itmal.answer.dto.AnswerUpdateRequest;
import com.itmal.answer.service.AnswerService;
import com.itmal.auth.repository.UserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    //필드 변수 추가. UserMApper 사용하도록 의존성 주입
    private final UserMapper userMapper;

    // 답변 목록 조회
    @GetMapping
    public String getAnswers(@RequestParam Long questionId, Model model) {
        model.addAttribute("answers", answerService.getAnswerByQuestionId(questionId));
        return "answers/list";
    }

    // 답변 작성
    @PostMapping
    public String createAnswer(@Valid @ModelAttribute AnswerCreateRequest request,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal UserDetails userDetails) {
        if (bindingResult.hasErrors()) {
            return "answers/write";
        }
        // 로그인한 사용자 ID를 서비스에 전달 (임시로 1L — 추후 실제 userId로 교체 -> 교체했음)
        answerService.createAnswer(request,getCurrentUserId(userDetails));
        return "redirect:/answers?questionId=" + request.getQuestionId();
    }

    // 수정 페이지
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("answer", answerService.getAnswer(id));
        return "answers/edit";
    }

    // 수정 처리
    @PostMapping("/{id}/edit")
    public String editAnswer(@PathVariable Long id,
                             @RequestParam Long questionId,
                             @Valid @ModelAttribute AnswerUpdateRequest request,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal UserDetails userDetails
                             ) {
        if (bindingResult.hasErrors()) {
            return "answers/edit";
        }
        // 임시로 1L — 추후 실제 userId로 교체 ->교체했음
        answerService.updateAnswer(id, request, getCurrentUserId(userDetails));
        return "redirect:/answers?questionId=" + questionId;
    }

    // 삭제
    @PostMapping("/{id}/delete")
    public String deleteAnswer(
            @PathVariable Long id, @RequestParam Long questionId,@AuthenticationPrincipal UserDetails userDetails
            ) {
        // 임시로 1L — 추후 실제 userId로 교체
        answerService.deleteAnswer(id, getCurrentUserId(userDetails));
        return "redirect:/answers?questionId=" + questionId;
    }

    // 좋아요
    @PostMapping("/{id}/like")
    public String likeAnswer(@PathVariable Long id, @RequestParam Long questionId, @AuthenticationPrincipal UserDetails userDetails) {
        // 임시로 1L — 추후 실제 userId로 교체 -> 교체했음
        answerService.toggleLike(id, getCurrentUserId(userDetails));
        return "redirect:/answers?questionId=" + questionId;
    }

    // 채택
    @PostMapping("/{id}/adopt")
    public String adoptAnswer(@PathVariable Long id, @RequestParam Long questionId, @AuthenticationPrincipal UserDetails userDetails) {
        // 임시로 1L — 추후 실제 userId로 교체
        answerService.adoptAnswer(id, getCurrentUserId(userDetails));
        return "redirect:/answers?questionId=" + questionId;
    }

    // 신고
    @PostMapping("/{id}/report")
    public String reportAnswer(@PathVariable Long id, @RequestParam Long questionId) {
        return "redirect:/answers?questionId=" + questionId;
    }

    //UserId 꺼내기
    private  Long getCurrentUserId(UserDetails userDetails) {
        return userMapper.findByEmail(userDetails.getUsername()).orElseThrow().getUserId();
    }
}
