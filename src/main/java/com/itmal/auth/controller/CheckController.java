package com.itmal.auth.controller;

import com.itmal.auth.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/check")
@RequiredArgsConstructor
public class CheckController {

    private final UserMapper userMapper;

    @GetMapping("/email")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam String email) {
        boolean isDuplicate = userMapper.existsByEmail(email);
        return ResponseEntity.ok(Map.of("duplicate", isDuplicate));
    }

    @GetMapping("/nickname")
    public ResponseEntity<Map<String, Boolean>> checkNickname(@RequestParam String nickname) {
        boolean isDuplicate = userMapper.existsByNickname(nickname);
        return ResponseEntity.ok(Map.of("duplicate", isDuplicate));
    }
}
