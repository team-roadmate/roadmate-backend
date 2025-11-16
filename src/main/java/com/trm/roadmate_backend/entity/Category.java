package com.trm.roadmate_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "category")
@Getter
@NoArgsConstructor // JPA 사용을 위한 기본 생성자
@AllArgsConstructor // 모든 필드를 인자로 받는 생성자
@Builder // 객체 생성을 위한 빌더 패턴 지원
// @Setter가 없어 불변성을 유지합니다.
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer categoryId;

    @Column(nullable = false)
    private String name;
}
