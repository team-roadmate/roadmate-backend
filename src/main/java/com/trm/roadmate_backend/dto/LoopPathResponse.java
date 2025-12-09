package com.trm.roadmate_backend.dto;

import lombok.*;

// ==================== 4. LoopPathResponse ====================
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoopPathResponse {
    private Double actualDistance;        // 실제 거리 (km)
    private Double targetDistance;        // 목표 거리 (km)
    private Double tolerance;             // 오차 (km)
    private Boolean withinTolerance;      // 허용 범위 내 여부
    private java.util.List<PathNode> path; // 경로 좌표 목록
    private SegmentInfo segment1;         // 출발→경유지 구간
    private SegmentInfo segment2;         // 경유지→출발 구간
    private String message;               // 안내 메시지

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SegmentInfo {
        private String from;              // 시작점 (S 또는 V)
        private String to;                // 도착점 (V 또는 S)
        private Double distance;          // 구간 거리 (km)
        private Integer nodeCount;        // 노드 개수
    }
}
