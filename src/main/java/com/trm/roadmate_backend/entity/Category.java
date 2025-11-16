package com.trm.roadmate_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;    // 🌟 추가
import lombok.AllArgsConstructor; // 🌟 추가
import lombok.Builder;            // 🌟 빌더 패턴을 위해 추가 (선택 사항이지만 권장)

@Entity
@Table(name = "category")
@Getter
@NoArgsConstructor // 🌟 JPA를 위한 기본 생성자
@AllArgsConstructor // 🌟 모든 필드를 사용하는 생성자
@Builder // 🌟 객체 생성 시 가독성을 높여주는 패턴
// 🌟 @Setter 제거: 정적인 데이터이므로 불변성을 확보합니다.
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer categoryId;

    @Column(nullable = false) // 🌟 이름 필드는 필수 값으로 지정 권장
    private String name;
}