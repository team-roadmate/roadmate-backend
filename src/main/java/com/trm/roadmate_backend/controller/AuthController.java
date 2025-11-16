package com.trm.roadmate_backend.controller;

import com.trm.roadmate_backend.config.JwtUtil;
import com.trm.roadmate_backend.dto.SignupRequest;
import com.trm.roadmate_backend.dto.LoginRequest;
import com.trm.roadmate_backend.dto.common.ApiResponse;
import com.trm.roadmate_backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@RequestBody SignupRequest request) { // 🌟 Void를 사용하여 데이터가 없음을 명시
        userService.registerUser(request.getName(), request.getEmail(), request.getPassword());

        // 🌟 ResponseEntity와 ApiResponse를 사용하여 표준 응답 반환
        return ResponseEntity.ok(ApiResponse.success("회원가입 성공", null));
    }

    @PostMapping("/login")
    // 🌟 로그인 시 토큰을 Map 형태로 반환하도록 Generic 타입 지정
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@RequestBody LoginRequest request) {
        var user = userService.login(request.getEmail(), request.getPassword());
        String token = jwtUtil.generateToken(user.getEmail());

        // 🌟 토큰을 Map에 담아 반환하여 JSON 키를 명확하게 지정
        Map<String, String> tokenMap = Map.of("accessToken", token);

        return ResponseEntity.ok(ApiResponse.success("로그인 성공", tokenMap));
    }

    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> test() {
        return ResponseEntity.ok(ApiResponse.success("접속 테스트 성공", "ok"));
    }
}
