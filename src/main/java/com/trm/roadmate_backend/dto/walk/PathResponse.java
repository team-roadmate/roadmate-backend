package com.trm.roadmate_backend.dto.walk;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PathResponse {
    private final List<Coordinate> path;
    private final double distance;
    private final int duration;
    private final String message;
}