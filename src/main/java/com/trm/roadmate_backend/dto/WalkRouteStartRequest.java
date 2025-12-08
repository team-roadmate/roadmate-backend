package com.trm.roadmate_backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WalkRouteStartRequest {
    private Float expectedDistance;
    private Integer expectedDuration;
    private String pathData; // 예정 경로 좌표 (JSON String)
}