package com.trm.roadmate_backend.controller;

import com.trm.roadmate_backend.config.JwtUtil;
import com.trm.roadmate_backend.dto.SignupRequest;
import com.trm.roadmate_backend.dto.LoginRequest;
import com.trm.roadmate_backend.dto.common.ApiResponse;
import com.trm.roadmate_backend.service.UserService;

import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@RequestBody SignupRequest request) {
        userService.registerUser(request.getName(), request.getEmail(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.success("회원가입 성공", null));
    }

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하여 Access/Refresh Token을 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@RequestBody LoginRequest request) {
        var user = userService.login(request.getEmail(), request.getPassword());

        String accessToken = jwtUtil.generateAccessToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        Map<String, String> tokenMap = Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        );

        return ResponseEntity.ok(ApiResponse.success("로그인 성공", tokenMap));
    }

    @Operation(summary = "Access Token 재발급", description = "Refresh Token을 이용해 새로운 Access Token을 발급합니다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("REFRESH_TOKEN_MISSING", "리프레시 토큰이 누락되었습니다."));
        }

        try {
            if (!jwtUtil.validateToken(refreshToken)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("REFRESH_TOKEN_INVALID", "리프레시 토큰이 유효하지 않거나 만료되었습니다."));
            }

            String email = jwtUtil.extractEmail(refreshToken);
            String newAccessToken = jwtUtil.generateAccessToken(email);

            Map<String, String> tokenMap = new HashMap<>();
            tokenMap.put("accessToken", newAccessToken);

            return ResponseEntity.ok(ApiResponse.success("새 액세스 토큰이 발급되었습니다.", tokenMap));

        } catch (JwtException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("TOKEN_PARSING_ERROR", "토큰 파싱 오류: 유효하지 않은 토큰 형식입니다."));
        }
    }

    @Operation(summary = "JWT 테스트", description = "JWT 인증이 정상적으로 동작하는지 확인하는 테스트용 API입니다.")
    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> test() {
        return ResponseEntity.ok(ApiResponse.success("접속 테스트 성공", "ok"));
    }
}
