package com.trm.roadmate_backend.controller;

import com.trm.roadmate_backend.config.JwtUtil;
import com.trm.roadmate_backend.dto.SignupRequest;
import com.trm.roadmate_backend.dto.LoginRequest;
import com.trm.roadmate_backend.dto.common.ApiResponse;
import com.trm.roadmate_backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import lombok.RequiredArgsConstructor; // Lombok을 사용하여 생성자 주입 간결화

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor // final 필드에 대한 생성자 주입
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * [POST] /api/auth/signup : 회원가입 엔드포인트
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@RequestBody SignupRequest request) {
        // UserService를 통해 사용자 등록 (비밀번호 암호화 포함)
        userService.registerUser(request.getName(), request.getEmail(), request.getPassword());

        // API 표준 응답 (200 OK, 데이터 없음)
        return ResponseEntity.ok(ApiResponse.success("회원가입 성공", null));
    }

    /**
     * [POST] /api/auth/login : 로그인 엔드포인트 (JWT 토큰 발급)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@RequestBody LoginRequest request) {
        // 1. UserService를 통해 사용자 인증
        var user = userService.login(request.getEmail(), request.getPassword());

        // 2. JWT 토큰 생성
        String token = jwtUtil.generateToken(user.getEmail());

        // 3. 토큰을 Map에 담아 반환 (키: accessToken)
        Map<String, String> tokenMap = Map.of("accessToken", token);

        // API 표준 응답 (200 OK, 토큰 데이터 포함)
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", tokenMap));
    }

    /**
     * [GET] /api/auth/test : JWT 인증 테스트 엔드포인트
     * SecurityConfig에 의해 인증된 사용자만 접근 가능
     */
    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> test() {
        return ResponseEntity.ok(ApiResponse.success("접속 테스트 성공", "ok"));
    }
}
