package com.itmal.answer.controller;

import com.itmal.answer.domain.Answer;
import com.itmal.answer.dto.AnswerCreateRequest;
import com.itmal.answer.dto.AnswerResponse;
import com.itmal.answer.dto.AnswerUpdateRequest;
import com.itmal.answer.service.AnswerService;
import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.auth.domain.Role;
import com.itmal.auth.domain.User;
import com.itmal.auth.repository.UserMapper;
import com.itmal.global.exception.BusinessException;
import com.itmal.global.exception.ErrorCode;
import com.itmal.global.exception.ViewException;
import com.itmal.question.dto.Target;
import com.itmal.question.mapper.QuestionMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    // userId 조회를 위한 UserMapper
    private final UserMapper userMapper;

    private final QuestionMapper questionMapper;

    // 답변 목록 조회 (JSON API - 질문 상세 페이지 fetch용)
    @GetMapping
    @ResponseBody
    public ResponseEntity<List<AnswerResponse>> getAnswers(@RequestParam Long questionId,
                                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        return ResponseEntity.ok(answerService.getAnswerResponsesByQuestionId(questionId, userId));
    }

    // 답변 작성
    @PostMapping
    public String createAnswer(@Valid @ModelAttribute AnswerCreateRequest request,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal UserDetails userDetails) {
        if (bindingResult.hasErrors()) {
            return "answer/write";
        }
        answerService.createAnswer(request, getCurrentUserId(userDetails));
        return "redirect:/questions/" + request.getQuestionId();
    }

    // 수정 페이지
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @RequestParam Long questionId,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model) {
        Answer answer = answerService.getAnswer(id);
        if (!answer.getUserId().equals(getCurrentUserId(userDetails))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        model.addAttribute("answer", answer);
        model.addAttribute("questionId", questionId);
        model.addAttribute("answerUpdateRequest", new AnswerUpdateRequest());
        return "answers/edit";
    }

    // 수정 처리
    @PostMapping("/{id}/edit")
    public String editAnswer(@PathVariable Long id,
                             @RequestParam Long questionId,
                             @Valid @ModelAttribute AnswerUpdateRequest request,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model) {
        if (bindingResult.hasErrors()) {
            var answer = answerService.getAnswer(id);
            answer.setContent(request.getContent());
            model.addAttribute("answer", answer);
            model.addAttribute("questionId", questionId);
            return "answers/edit";
        }
        answerService.updateAnswer(id, request, getCurrentUserId(userDetails));
        return "redirect:/questions/" + questionId;
    }

    // 삭제
    @PostMapping("/{id}/delete")
    public String deleteAnswer(
            @PathVariable Long id,
            @RequestParam Long questionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        answerService.deleteAnswer(id, getCurrentUserId(userDetails));
        return "redirect:/questions/" + questionId;
    }

    // 좋아요
    @PostMapping("/{id}/like")
    public String likeAnswer(@PathVariable Long id, @RequestParam Long questionId, @AuthenticationPrincipal UserDetails userDetails) {
        answerService.toggleLike(id, getCurrentUserId(userDetails));
        return "redirect:/questions/" + questionId;
    }

    // 채택
    @PostMapping("/{id}/adopt")
    public String adoptAnswer(@PathVariable Long id, @RequestParam Long questionId, @AuthenticationPrincipal UserDetails userDetails) {
        answerService.adoptAnswer(id, getCurrentUserId(userDetails));
        return "redirect:/questions/" + questionId;
    }

    // 신고
    @PostMapping("/{id}/report")
    public String reportAnswer(@PathVariable Long id,
                               @RequestParam Long questionId,
                               @AuthenticationPrincipal UserDetails userDetails) {
        // TODO: 신고 기능 구현 예정
        return "redirect:/questions/" + questionId;
    }

    private Long getCurrentUserId(UserDetails userDetails) {
        return userMapper.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND))
                .getUserId();
    }

    //답변 작성 페이지
    @GetMapping("/write")
    public String writePage(@RequestParam Long questionId,
                            @AuthenticationPrincipal UserDetails userDetails,
                            Model model) {
        var question = questionMapper.findQuestionDetailById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
        }
        if (Target.TUTOR.getCode().equals(question.getTarget())) {
            User user = userMapper.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new ViewException(ErrorCode.USER_NOT_FOUND));
            if (user.getRole() != Role.ROLE_TUTOR) {
                throw new ViewException(ErrorCode.FORBIDDEN);
            }
        }
        model.addAttribute("questionId", questionId);
        model.addAttribute("question", question);
        return "answer/write";
    }
}
