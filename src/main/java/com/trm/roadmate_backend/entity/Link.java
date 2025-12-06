package com.trm.roadmate_backend.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "link")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Link {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 내부 Primary Key

    @Column(nullable = false, unique = true, length = 100)
    private String linkId; // 링크 고유 ID (API 데이터)

    @Column(nullable = false, length = 50)
    private String startNodeId; // 시작 노드 ID

    @Column(nullable = false, length = 50)
    private String endNodeId; // 끝 노드 ID

    private Double length; // 길이

    @Column(length = 10)
    private String typeCd; // 링크 유형 코드

    @Column(columnDefinition = "TEXT")
    private String geometry; // 기하 정보 (WKT 형식)

    @Column(length = 10)
    private String sggCd; // 시군구 코드

    @Column(length = 50)
    private String sggNm; // 시군구 명

    @Column(length = 10)
    private String emdCd; // 읍면동 코드

    @Column(length = 50)
    private String emdNm; // 읍면동 명

    @Column(length = 1)
    private String expnCarRd; // 확장 차도

    @Column(length = 1)
    private String sbwyNtw; // 지하철 네트워크

    @Column(length = 1)
    private String brg; // 교량

    @Column(length = 1)
    private String tnl; // 터널

    @Column(length = 1)
    private String ovrp; // 고가도로

    @Column(length = 1)
    private String crswk; // 횡단보도

    @Column(length = 1)
    private String park; // 공원

    @Column(length = 1)
    private String bldg; // 빌딩

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // 생성 시간
}