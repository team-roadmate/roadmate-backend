package com.trm.roadmate_backend.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "node")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Node {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 내부 Primary Key

    @Column(nullable = false, unique = true, length = 50)
    private String nodeId; // 노드 고유 ID (API 데이터)

    @Column(nullable = false)
    private Double latitude; // 위도

    @Column(nullable = false)
    private Double longitude; // 경도

    @Column(length = 10)
    private String nodeTypeCd; // 노드 유형 코드

    @Column(length = 10)
    private String sggCd; // 시군구 코드

    @Column(length = 50)
    private String sggNm; // 시군구 명

    @Column(length = 10)
    private String emdCd; // 읍면동 코드

    @Column(length = 50)
    private String emdNm; // 읍면동 명

    @Column(nullable = false)
    private Boolean isVirtual = false; // 가상 노드 여부

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // 생성 시간
}