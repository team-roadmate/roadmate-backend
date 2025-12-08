package com.trm.roadmate_backend.controller;

import com.trm.roadmate_backend.dto.PathRequest;
import com.trm.roadmate_backend.dto.PathResult;
import com.trm.roadmate_backend.service.PathfindingService;

import io.swagger.v3.oas.annotations.Operation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/path")
@RequiredArgsConstructor
@Slf4j
public class PathfindingController {

    private final PathfindingService pathfindingService;

    // =============================
    // 1. GET 방식: 쿼리 파라미터 기반
    // =============================
    @Operation(
            summary = "최단 경로 조회 (GET)",
            description = "쿼리 파라미터(startLat, startLon, endLat, endLon)를 통해 최단 경로를 계산합니다."
    )
    @GetMapping("/shortest")
    public ResponseEntity<PathResult> getShortestPath(
            @RequestParam("startLat") double startLat,
            @RequestParam("startLon") double startLon,
            @RequestParam("endLat") double endLat,
            @RequestParam("endLon") double endLon) {

        log.info("Pathfinding Request (GET) received: Start({}, {}) -> End({}, {})",
                startLat, startLon, endLat, endLon);

        PathResult result = pathfindingService.findShortestPathByCoords(
                startLat, startLon, endLat, endLon
        );

        return processPathResult(result);
    }

    // =============================
    // 2. POST 방식: JSON Body 기반
    // =============================
    @Operation(
            summary = "최단 경로 조회 (POST)",
            description = "JSON Body(PathRequest)를 통해 최단 경로를 계산합니다."
    )
    @PostMapping("/shortest")
    public ResponseEntity<PathResult> postShortestPath(@RequestBody PathRequest request) {

        log.info("Pathfinding Request (POST) received: Start({}, {}) -> End({}, {})",
                request.getStartLat(), request.getStartLon(), request.getEndLat(), request.getEndLon());

        PathResult result = pathfindingService.findShortestPathByCoords(
                request.getStartLat(),
                request.getStartLon(),
                request.getEndLat(),
                request.getEndLon()
        );

        return processPathResult(result);
    }

    // =============================
    // 3. 공통 결과 처리
    // =============================
    private ResponseEntity<PathResult> processPathResult(PathResult result) {
        if (result.getTotalDistance() > 0 && !result.getPath().isEmpty()) {
            log.info("Pathfinding Success: Distance = {}m, Path Length = {}",
                    result.getTotalDistance(), result.getPath().size());
            return ResponseEntity.ok(result);
        } else {
            log.warn("Pathfinding Failed: No path found or invalid node coordinates.");
            return ResponseEntity.ok(result);
        }
    }
}
