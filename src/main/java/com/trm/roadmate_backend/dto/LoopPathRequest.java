package com.trm.roadmate_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// ==================== 3. LoopPathRequest ====================
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LoopPathRequest {
    private Double startLat;
    private Double startLng;
    private Double viaLat;
    private Double viaLng;
    private Double targetDistanceKm;       // 목표 거리 (km)
    private Integer tolerancePercent = 15; // 오차 허용 (기본 15%)
    private String detourDirection = "auto"; // auto, north, south, east, west
}
