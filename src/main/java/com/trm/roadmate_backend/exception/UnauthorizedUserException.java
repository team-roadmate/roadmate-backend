package com.trm.roadmate_backend.exception;

// 403 Forbidden 응답을 유도하기 위한 커스텀 예외
public class UnauthorizedUserException extends RuntimeException {
    public UnauthorizedUserException(String message) {
        super(message);
    }
}