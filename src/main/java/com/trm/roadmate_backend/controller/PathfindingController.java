package com.trm.roadmate_backend.controller;

import com.trm.roadmate_backend.dto.PathRequest; // 💡 새로 추가된 요청 DTO
import com.trm.roadmate_backend.dto.PathResult;
import com.trm.roadmate_backend.service.PathfindingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping; // 💡 PostMapping 임포트
import org.springframework.web.bind.annotation.RequestBody; // 💡 RequestBody 임포트
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/path")
@RequiredArgsConstructor
@Slf4j
public class PathfindingController {

    private final PathfindingService pathfindingService;

    // --- 1. 기존 GET 메서드 유지 (쿼리 파라미터 사용) ---

    /**
     * @GET /api/path/shortest : 쿼리 파라미터로 좌표를 받습니다.
     */
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

    // --- 2. 새로운 POST 메서드 추가 (JSON Body 사용) ---

    /**
     * @POST /api/path/shortest : JSON 본문으로 좌표를 받습니다.
     */
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

    // --- 3. 공통 결과 처리 메서드 ---

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