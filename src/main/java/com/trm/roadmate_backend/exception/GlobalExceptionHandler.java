package com.trm.roadmate_backend.exception;

import com.trm.roadmate_backend.dto.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. [400 Bad Request] BadCredentialsException 처리 (로그인 실패)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    // 2. [409 Conflict] IllegalStateException 처리 (이메일 중복 등)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    // ⭐️ 3. [403 Forbidden] UnauthorizedUserException 처리 (경로 접근 권한/사용자 없음)
    @ExceptionHandler(UnauthorizedUserException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedUserException(UnauthorizedUserException ex) {
        return new ResponseEntity<>(
                ApiResponse.error("USER_403_INVALID", ex.getMessage()), // 에러 코드 사용 (예: USER_403_INVALID)
                HttpStatus.FORBIDDEN // 403 Forbidden 반환
        );
    }

    // ⭐️ 4. [500 Internal Server Error] 기타 잡히지 않은 모든 예외 처리
    // 이 핸들러는 위의 명시적인 핸들러들에서 처리되지 않은 모든 RuntimeException 및 Exception을 포괄합니다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        // 실제 배포 환경에서는 ex.getMessage()를 그대로 노출하지 않도록 주의해야 합니다.
        // 여기서는 디버깅 용이성을 위해 메시지를 포함합니다.
        return new ResponseEntity<>(
                ApiResponse.error("SERVER_500", "서버 처리 중 알 수 없는 오류가 발생했습니다: " + ex.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR // 500 Internal Server Error 반환
        );
    }
}