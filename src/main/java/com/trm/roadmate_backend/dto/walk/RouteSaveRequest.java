package com.trm.roadmate_backend.dto.walk;

import java.util.List;

public record RouteSaveRequest(
        Long userId,
        String title,
        String userMemo,

        List<Coordinate> pathCoordinates,

        double totalDistance,
        long durationSeconds
) {}