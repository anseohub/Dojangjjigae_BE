package com.dojangjjigae.controller;

import com.dojangjjigae.dto.LoginRequest;
import com.dojangjjigae.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 공모전 심사용 테스트 계정 하나만 로그인되게 고정한다 (회원가입 · 토큰 없음).
 * 모든 요청은 AppConstants.TEST_USER_ID 로 처리되므로 로그인 성공 여부만 확인한다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${app.auth.test-email}")
    private String testEmail;

    @Value("${app.auth.test-password}")
    private String testPassword;

    @Value("${app.auth.nickname}")
    private String nickname;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        boolean emailMatches = testEmail.equalsIgnoreCase(request.getEmail().trim());
        boolean passwordMatches = testPassword.equals(request.getPassword());

        if (!emailMatches || !passwordMatches) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "이메일 또는 비밀번호가 일치하지 않아요."));
        }

        return ResponseEntity.ok(LoginResponse.builder()
                .email(testEmail)
                .nickname(nickname)
                .build());
    }
}
