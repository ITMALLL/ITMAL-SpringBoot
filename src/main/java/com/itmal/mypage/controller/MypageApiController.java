package com.itmal.mypage.controller;

import com.itmal.answer.domain.Answer;
import com.itmal.answer.service.AnswerService;
import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.comment.dto.CommentResponseDto;
import com.itmal.comment.service.CommentService;
import com.itmal.mypage.mapper.MypageMapper;
import com.itmal.question.dto.QuestionDto;
import com.itmal.question.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/mypage")
public class MypageApiController {

    private final QuestionService questionService;
    private final AnswerService answerService;
    private final CommentService commentService;
    private final MypageMapper mypageMapper;

    @GetMapping("/questions")
    public List<QuestionDto> getQuestion(@AuthenticationPrincipal CustomUserDetails userDetails,
                                         @RequestParam(required = false) Long userId){
        if(userId == null && userDetails == null ){
            return null;
        }
        Long targetId = (userId != null) ? userId : userDetails.getUserId();
        return questionService.findByUserId(targetId);
    }

    @GetMapping("/answers")
    public List<Answer> getAnswer(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  @RequestParam(required = false) Long userId){
        if(userId == null && userDetails == null ){
            return null;
        }
        Long targetId = (userId != null) ? userId : userDetails.getUserId();
        return answerService.getAnswerByUserId(targetId);
    }

    @GetMapping("/comment")
    public List<CommentResponseDto> getMypageComment(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                     @RequestParam(required = false) Long userId){
        if(userId == null && userDetails == null ){
            return null;
        }
        Long targetId = (userId != null) ? userId : userDetails.getUserId();
        return commentService.getMypageComment(targetId);
    }

    @GetMapping("/countQuestions")
    public int countQuestions(@AuthenticationPrincipal CustomUserDetails userDetails,
                              @RequestParam(required = false) Long userId) {
        if(userId == null && userDetails == null ){
            return 0;
        }
        Long targetId = (userId != null) ? userId : userDetails.getUserId();
        return mypageMapper.countQuestionByUserId(targetId);
    }

    @GetMapping("/countAnswers")
    public int countAnswers(@AuthenticationPrincipal CustomUserDetails userDetails,
                            @RequestParam(required = false) Long userId) {
        if(userId == null && userDetails == null ){
            return 0;
        }
        Long targetId = (userId != null) ? userId : userDetails.getUserId();
        return mypageMapper.countAnswerByUserId(targetId);
    }

    @GetMapping("/countComments")
    public int countComments(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam(required = false) Long userId) {
        if(userId == null && userDetails == null ){
            return 0;
        }
        Long targetId = (userId != null) ? userId : userDetails.getUserId();
        return mypageMapper.countCommentByUserId(targetId);
    }






}
