package com.trm.roadmate_backend.controller;

import com.trm.roadmate_backend.config.JwtUtil;
import com.trm.roadmate_backend.dto.LoginRequest;
import com.trm.roadmate_backend.dto.SignupRequest;
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
import java.util.NoSuchElementException;

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
        String email = user.getEmail();

        String accessToken = jwtUtil.generateAccessToken(email);
        String refreshToken = jwtUtil.generateRefreshToken(email);

        // [수정된 로직] 로그인 시 새로 발급된 리프레시 토큰을 DB에 저장
        userService.saveRefreshToken(email, refreshToken);

        Map<String, String> tokenMap = Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        );

        return ResponseEntity.ok(ApiResponse.success("로그인 성공", tokenMap));
    }

    @Operation(summary = "Access Token 재발급", description = "Refresh Token을 이용해 새로운 Access Token을 발급합니다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refresh(@RequestBody Map<String, String> request) {
        String oldRefreshToken = request.get("refreshToken"); // 기존 리프레시 토큰

        if (oldRefreshToken == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("REFRESH_TOKEN_MISSING", "리프레시 토큰이 누락되었습니다."));
        }

        try {
            // 1. 토큰 자체의 유효성 검사 (만료, 서명)
            if (!jwtUtil.validateToken(oldRefreshToken)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("REFRESH_TOKEN_INVALID", "리프레시 토큰이 유효하지 않거나 만료되었습니다."));
            }

            String email = jwtUtil.extractEmail(oldRefreshToken);

            // 2. [추가된 로직] 서버 저장소에 저장된 토큰과 일치하는지 검증 (RTR의 핵심)
            if (!userService.validateStoredRefreshToken(email, oldRefreshToken)) {
                // 토큰이 DB의 것과 불일치하면 (이미 갱신되었거나 탈취된 경우) UNAUTHORIZED 반환
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("REFRESH_TOKEN_MISMATCH", "토큰이 이미 사용되었거나 서버와 불일치합니다."));
            }

            // 3. 새로운 액세스 및 리프레시 토큰 발급
            String newAccessToken = jwtUtil.generateAccessToken(email);
            String newRefreshToken = jwtUtil.generateRefreshToken(email);

            // 4. [추가된 로직] 새로 발급된 리프레시 토큰을 DB에 갱신/저장 (기존 토큰 자동 무효화)
            userService.saveRefreshToken(email, newRefreshToken);

            Map<String, String> tokenMap = new HashMap<>();
            tokenMap.put("accessToken", newAccessToken);
            tokenMap.put("refreshToken", newRefreshToken);

            return ResponseEntity.ok(ApiResponse.success("새 액세스 및 리프레시 토큰이 발급되었습니다.", tokenMap));

        } catch (JwtException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("TOKEN_PARSING_ERROR", "토큰 파싱 오류: 유효하지 않은 토큰 형식입니다."));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("USER_NOT_FOUND", "토큰과 연결된 사용자를 찾을 수 없습니다."));
        }
    }

    @Operation(summary = "JWT 테스트", description = "JWT 인증이 정상적으로 동작하는지 확인하는 테스트용 API입니다.")
    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> test() {
        return ResponseEntity.ok(ApiResponse.success("접속 테스트 성공", "ok"));
    }
}
