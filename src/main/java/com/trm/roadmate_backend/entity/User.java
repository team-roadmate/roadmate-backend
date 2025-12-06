package com.trm.roadmate_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "user")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /**
     * 비밀번호 필드: @JsonIgnore를 통해 JSON 응답 시 자동 제외 (보안 강화)
     */
    @Column(nullable = false)
    @JsonIgnore
    private String password;

    private Integer age;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    /**
     * 생성 시간: 최초 저장 시 자동 설정되며, 업데이트 불가능
     */
    @Column(name = "created_at", updatable = false)
    @Getter // @Setter는 제거되어 불변성을 확보합니다.
    private LocalDateTime createdAt;

    // 엔티티 저장 전(PrePersist)에 현재 시간을 생성 시간에 설정
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
