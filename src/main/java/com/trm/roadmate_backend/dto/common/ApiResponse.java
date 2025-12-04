package com.trm.roadmate_backend.dto.common;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiResponse<T> {

    private final boolean success; // API 요청 성공 여부
    private final String message;  // 응답 메시지
    private final String errorCode; // 에러 코드 (선택 사항, 실패 시 사용)
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
                .errorCode(null) // 성공 시 에러 코드는 null
                .data(data)
                .build();
    }

    /**
     * 실패 응답을 생성하는 정적 팩토리 메서드 (에러 코드 없음)
     * @param message 응답 메시지 (주로 에러 메시지)
     * @param <T> 데이터의 타입
     * @return 실패 응답 객체
     */
    public static <T> ApiResponse<T> failure(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(null) // 에러 코드가 없는 경우
                .data(null)
                .build();
    }

    /**
     * 실패 응답을 생성하는 정적 팩토리 메서드 (에러 코드 포함)
     * @param errorCode 내부적으로 정의된 에러 코드 (예: "TOKEN_401", "USER_NOT_FOUND")
     * @param message 응답 메시지 (주로 에러 메시지)
     * @param <T> 데이터의 타입
     * @return 실패 응답 객체
     * **신규 추가된 메서드입니다.**
     */
    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .data(null)
                .build();
    }
}