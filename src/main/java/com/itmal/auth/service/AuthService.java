package com.itmal.auth.service;

import com.itmal.auth.domain.Role;
import com.itmal.auth.domain.User;
import com.itmal.auth.dto.RegisterRequest;
import com.itmal.auth.exception.DuplicateEmailException;
import com.itmal.auth.exception.DuplicateNicknameException;
import com.itmal.auth.repository.UserMapper;
import com.itmal.global.exception.ApiException;
import com.itmal.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegisterRequest request) {
        if (userMapper.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException();
        }

        if (userMapper.existsByNickname(request.getNickname())) {
            throw new DuplicateNicknameException();
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .nativeLanguage(request.getNativeLanguage())
                .role(Role.ROLE_USER)
                .emailVerified(false)
                .build();
        try {
            userMapper.insert(user); // useGeneratedKeys → user.getUserId() 자동 세팅
        } catch (DuplicateKeyException e) {
            // 동시 요청 race condition: 트랜잭션 내 재조회는 REPEATABLE READ 스냅샷으로 잘못 분기될 수 있어
            // 사전 체크(existsByEmail/existsByNickname)가 통과된 시점의 충돌이므로 공통 409로 처리
            throw new ApiException(ErrorCode.DUPLICATE_RESOURCE);
        }
        userService.registerLearningLanguages(user.getUserId(), request.getLearningLanguages());
    }
}
