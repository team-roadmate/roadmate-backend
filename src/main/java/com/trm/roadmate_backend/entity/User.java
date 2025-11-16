package com.trm.roadmate_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore; // 🌟 1번 개선: 보안을 위해 추가

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

    // 🌟 1번 개선: 민감한 정보인 비밀번호가 JSON 응답에 포함되지 않도록 설정
    @Column(nullable = false)
    @JsonIgnore
    private String password;

    private Integer age;

    // 🌟 2번 개선: FetchType을 LAZY로 변경하여 불필요한 조회를 방지합니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_category", foreignKey = @ForeignKey(name = "fk_user_category"))
    private Category preferredCategory;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    // 🌟 3번 개선: 생성 시간은 수정되면 안 되므로 @Setter를 제거합니다.
    @Column(name = "created_at", updatable = false)
    @Getter // 읽기만 가능하도록
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}