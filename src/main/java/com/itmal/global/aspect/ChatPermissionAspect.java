package com.itmal.global.aspect;

import com.itmal.auth.domain.Role;
import com.itmal.auth.domain.User;
import com.itmal.auth.repository.UserMapper;
import com.itmal.chat.dto.ChatRequestDto;
import com.itmal.global.exception.ApiException;
import com.itmal.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class ChatPermissionAspect {

    private final UserMapper userMapper;

    @Before("execution(* com.itmal.chat.service.ChatRequestService.createChatRequest(..)) && args(chatRequest)")
    public void checkChatRequestPermission(ChatRequestDto chatRequest) {
        User responder = userMapper.findById(chatRequest.getResponderId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (responder.getRole() != Role.ROLE_TUTOR) {
            throw new ApiException(ErrorCode.CHAT_INVALID_TARGET);
        }
    }
}
