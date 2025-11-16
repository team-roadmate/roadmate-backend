package com.trm.roadmate_backend.dto;

import lombok.Getter;
import lombok.Setter;

// 회원가입 요청 시 사용되는 DTO
@Getter
@Setter
public class SignupRequest {
    private String name;
    private String email;
    private String password;
}
