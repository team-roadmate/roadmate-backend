package com.trm.roadmate_backend.dto;

import lombok.*;

// ==================== 1. LoopEstimateRequest ====================
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LoopEstimateRequest {
    private Double startLat;
    private Double startLng;
    private Double viaLat;
    private Double viaLng;
}

