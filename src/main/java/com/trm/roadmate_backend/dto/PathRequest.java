package com.trm.roadmate_backend.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * POST 요청 본문에서 좌표를 받기 위한 DTO
 */
@Getter
@RequiredArgsConstructor
public class PathRequest {
    private final double startLat;
    private final double startLon;
    private final double endLat;
    private final double endLon;
}