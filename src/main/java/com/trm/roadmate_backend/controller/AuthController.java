package com.trm.roadmate_backend.controller;

import com.trm.roadmate_backend.config.JwtUtil;
import com.trm.roadmate_backend.dto.SignupRequest;
import com.trm.roadmate_backend.dto.LoginRequest;
import com.trm.roadmate_backend.dto.common.ApiResponse; // ApiResponse 임포트
import com.trm.roadmate_backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.jsonwebtoken.JwtException;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * [POST] /api/auth/signup : 회원가입 엔드포인트
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@RequestBody SignupRequest request) {
        userService.registerUser(request.getName(), request.getEmail(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.success("회원가입 성공", null));
    }

    /**
     * [POST] /api/auth/login : 로그인 엔드포인트 (ACCESS + REFRESH 토큰 발급)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@RequestBody LoginRequest request) {
        var user = userService.login(request.getEmail(), request.getPassword());

        String accessToken = jwtUtil.generateAccessToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // TODO: [중요] 실제 운영 환경에서는, 발급된 Refresh Token을 사용자 정보와 함께 DB 또는 Redis에 저장하여 관리해야 합니다.

        Map<String, String> tokenMap = Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        );

        return ResponseEntity.ok(ApiResponse.success("로그인 성공", tokenMap));
    }

    /**
     * [POST] /api/auth/refresh : 액세스 토큰 재발급 엔드포인트
     * **수정 부분:** ApiResponse.error() 사용
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    // ApiResponse.error()를 사용하여 에러 코드 포함
                    .body(ApiResponse.error("REFRESH_TOKEN_MISSING", "리프레시 토큰이 누락되었습니다."));
        }

        try {
            if (!jwtUtil.validateToken(refreshToken)) {
                // 토큰 만료 또는 변조 시
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED) // 401 Unauthorized
                        // ApiResponse.error()를 사용하여 에러 코드 포함
                        .body(ApiResponse.error("REFRESH_TOKEN_INVALID", "리프레시 토큰이 유효하지 않거나 만료되었습니다. 재로그인이 필요합니다."));
            }

            String email = jwtUtil.extractEmail(refreshToken);

            // TODO: [중요] 실제 운영 환경에서는, 추출된 이메일을 기반으로 DB/Redis에 저장된 Refresh Token과 일치하는지 확인해야 합니다.

            String newAccessToken = jwtUtil.generateAccessToken(email);

            Map<String, String> tokenMap = new HashMap<>();
            tokenMap.put("accessToken", newAccessToken);

            return ResponseEntity.ok(ApiResponse.success("새 액세스 토큰이 발급되었습니다.", tokenMap));

        } catch (JwtException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    // ApiResponse.error()를 사용하여 에러 코드 포함
                    .body(ApiResponse.error("TOKEN_PARSING_ERROR", "토큰 파싱 오류: 유효하지 않은 토큰 형식입니다."));
        }
    }


    /**
     * [GET] /api/auth/test : JWT 인증 테스트 엔드포인트
     */
    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> test() {
        return ResponseEntity.ok(ApiResponse.success("접속 테스트 성공", "ok"));
    }
}