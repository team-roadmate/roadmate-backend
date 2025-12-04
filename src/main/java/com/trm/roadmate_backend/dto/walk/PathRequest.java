package com.trm.roadmate_backend.dto.walk;

import lombok.*;

// ===== 경로 탐색 요청 DTO =====
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PathRequest {
    private Double startLat;
    private Double startLng;
    private Double endLat;
    private Double endLng;

    // 옵션
    private Boolean preferPark = false;      // 공원 선호
    private Boolean avoidOverpass = false;   // 육교 피하기
    private Boolean avoidTunnel = false;     // 터널 피하기
    private Boolean preferIndoor = false;    // 실내 선호 (비올 때)
}
