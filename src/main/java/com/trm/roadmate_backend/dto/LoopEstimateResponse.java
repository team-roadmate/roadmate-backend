package com.trm.roadmate_backend.dto;

import lombok.*;

// ==================== 2. LoopEstimateResponse ====================
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoopEstimateResponse {
    private Double minLoopDistance;       // 최소 루프 거리 (km)
    private Double straightDistance;      // 직선거리 (km)
    private Double recommendedMin;        // 권장 최소 (km)
    private Double recommendedMax;        // 권장 최대 (km)
    private Boolean feasible;             // 실행 가능 여부
    private String message;               // 안내 메시지
}
