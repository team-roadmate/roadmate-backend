package com.trm.roadmate_backend.dto;

import lombok.Data;

@Data
public class PathRequest {
    private Double startLat;
    private Double startLng;
    private Double endLat;
    private Double endLng;
}