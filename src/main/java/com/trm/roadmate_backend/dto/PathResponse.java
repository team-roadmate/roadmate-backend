package com.trm.roadmate_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PathResponse {
    private Double totalDistance;  // 미터
    private Integer estimatedTime;  // 초 (보행속도 4km/h 기준)
    private List<PathNode> path;
    private List<PathLink> links;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathNode {
        private String nodeId;
        private Double latitude;
        private Double longitude;
        private String nodeType;  // 일반, 지하철출구, 버스정류장 등
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathLink {
        private String linkId;
        private Double length;
        private String roadType;  // 일반도로, 공원, 지하도 등
    }
}