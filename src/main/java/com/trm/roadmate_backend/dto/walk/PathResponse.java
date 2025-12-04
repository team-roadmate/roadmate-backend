package com.trm.roadmate_backend.dto.walk;

import lombok.*;

import java.util.List;

// ===== 경로 탐색 응답 DTO =====
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PathResponse {
    private List<Coordinate> path;     // 경로 좌표들
    private Double distance;            // 총 거리 (미터)
    private Integer duration;           // 예상 시간 (초)
    private String message;             // 메시지
}
