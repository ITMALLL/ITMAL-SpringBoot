package com.itmal.global.aspect;

import com.itmal.answer.dto.AnswerCreateRequest;
import com.itmal.auth.domain.Role;
import com.itmal.auth.domain.User;
import com.itmal.auth.repository.UserMapper;
import com.itmal.global.exception.ErrorCode;
import com.itmal.global.exception.ViewException;
import com.itmal.question.dto.QuestionDto;
import com.itmal.question.dto.Target;
import com.itmal.question.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AnswerPermissionAspect {

    private final QuestionMapper questionMapper;
    private final UserMapper userMapper;

    @Before("execution(* com.itmal.answer.service.AnswerServiceImpl.createAnswer(..)) && args(request, userId)")
    public void checkAnswerPermission(AnswerCreateRequest request, Long userId) {
        QuestionDto question = questionMapper.findQuestionDetailById(request.getQuestionId());
        if (question != null && Target.TUTOR.getCode().equals(question.getTarget())) {
            User user = userMapper.findById(userId)
                    .orElseThrow(() -> new ViewException(ErrorCode.USER_NOT_FOUND));
            if (user.getRole() != Role.ROLE_TUTOR) {
                throw new ViewException(ErrorCode.FORBIDDEN);
            }
        }
    }
}
