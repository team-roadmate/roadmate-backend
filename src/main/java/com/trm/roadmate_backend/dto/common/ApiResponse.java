package com.trm.roadmate_backend.dto.common;

import lombok.Builder;
import lombok.Getter;

// 모든 API 응답의 표준이 될 DTO입니다.
@Getter
@Builder
public class ApiResponse<T> {
    private final boolean success; // 성공 여부 (true/false)
    private final String message;  // 응답 메시지 (예: "회원가입 성공")
    private final T data;          // 실제 응답 데이터 (예: JWT 토큰, 사용자 정보)

    // 성공 응답을 위한 정적 팩토리 메서드
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    // 에러 응답을 위한 정적 팩토리 메서드
    public static <T> ApiResponse<T> failure(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}
