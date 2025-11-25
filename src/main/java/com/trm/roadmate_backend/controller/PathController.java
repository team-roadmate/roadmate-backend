package com.trm.roadmate_backend.controller;

import com.trm.roadmate_backend.dto.walk.PathRequest;
import com.trm.roadmate_backend.dto.walk.PathResponse;
import com.trm.roadmate_backend.service.walk.PathFindingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; /**
 * 경로 탐색 API
 */
@RestController
@RequestMapping("/api/paths")
@RequiredArgsConstructor
@Slf4j
public class PathController {

    private final PathFindingService pathFindingService;

    /**
     * 경로 탐색
     * POST /api/paths/search
     */
    @PostMapping("/search")
    public ResponseEntity<PathResponse> searchPath(@RequestBody PathRequest request) {
        log.info("경로 탐색 요청: ({}, {}) → ({}, {})",
                request.getStartLat(), request.getStartLng(),
                request.getEndLat(), request.getEndLng());

        PathResponse response = pathFindingService.findPath(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 헬스체크
     * GET /api/paths/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
