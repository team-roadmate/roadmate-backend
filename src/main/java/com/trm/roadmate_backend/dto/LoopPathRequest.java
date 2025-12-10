package com.trm.roadmate_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LoopPathRequest {
    private Double startLat;              // 시작 지점 위도
    private Double startLng;              // 시작 지점 경도
    private Double viaLat;                // 중간 경유지 위도
    private Double viaLng;                // 중간 경유지 경도
    private Double targetDistanceKm;      // 목표 거리 (km)
    private Integer tolerancePercent = 15; // 오차 허용 (기본 15%)
}
