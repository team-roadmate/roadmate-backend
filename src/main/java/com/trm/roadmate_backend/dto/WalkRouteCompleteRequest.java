package com.trm.roadmate_backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WalkRouteCompleteRequest {
    private Float distance; // 실제 이동 거리
    private Integer duration; // 실제 소요 시간
}