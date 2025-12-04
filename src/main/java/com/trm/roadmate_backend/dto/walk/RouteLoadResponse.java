package com.trm.roadmate_backend.dto.walk;

import com.trm.roadmate_backend.entity.walk.WalkingRoute;
import java.time.LocalDateTime;
import java.util.List;

public record RouteLoadResponse(
        Long routeId,
        Long userId,
        String title,
        String userMemo,

        List<Coordinate> pathCoordinates,

        double totalDistance,
        long durationSeconds,
        LocalDateTime savedAt,
        boolean isCompleted
        // startNodeId, endNodeId 필드 제거
) {
    public static RouteLoadResponse from(WalkingRoute route, List<Coordinate> coordinates) {
        return new RouteLoadResponse(
                route.getId(),
                route.getUserId(),
                route.getTitle(),
                route.getUserMemo(),
                coordinates,
                route.getTotalDistance(),
                route.getDurationSeconds(),
                route.getSavedAt(),
                route.isCompleted()
                // startNodeId, endNodeId 필드 제거
        );
    }
}