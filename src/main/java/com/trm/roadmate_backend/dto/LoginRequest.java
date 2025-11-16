package com.trm.roadmate_backend.dto;

import lombok.Getter;
import lombok.Setter;

// 로그인 요청 시 사용되는 DTO
@Getter
@Setter
public class LoginRequest {
    private String email;
    private String password;
}
