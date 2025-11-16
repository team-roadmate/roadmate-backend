package com.trm.roadmate_backend.dto.common;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiResponse<T> {

    private final boolean success; // API 요청 성공 여부
    private final String message;  // 응답 메시지
    private final T data;          // 실제 응답 데이터

    /**
     * 성공 응답을 생성하는 정적 팩토리 메서드
     * @param message 응답 메시지
     * @param data 반환할 실제 데이터
     * @param <T> 데이터의 타입
     * @return 성공 응답 객체
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 실패 응답을 생성하는 정적 팩토리 메서드 (데이터는 null로 설정)
     * @param message 응답 메시지 (주로 에러 메시지)
     * @param <T> 데이터의 타입
     * @return 실패 응답 객체
     */
    public static <T> ApiResponse<T> failure(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}
