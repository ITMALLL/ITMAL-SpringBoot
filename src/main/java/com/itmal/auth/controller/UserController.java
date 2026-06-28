package com.itmal.auth.controller;

import com.itmal.auth.dto.UserResponse;
import com.itmal.auth.service.UserService;
import com.itmal.global.common.ApiResponse;
import com.itmal.global.exception.ApiException;
import com.itmal.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long userId) {
        try {
            UserResponse response = UserResponse.from(userService.findById(userId));
            return ResponseEntity.ok(new ApiResponse<>("200", "조회 성공", response));
        } catch (NoSuchElementException e) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
    }
}
