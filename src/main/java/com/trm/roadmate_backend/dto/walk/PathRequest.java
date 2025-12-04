package com.trm.roadmate_backend.dto.walk;

import lombok.*;

// ===== 경로 탐색 요청 DTO (A* 최단 경로용) =====
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PathRequest {
    private Double startLat;
    private Double startLng;
    private Double endLat;
    private Double endLng;
}
