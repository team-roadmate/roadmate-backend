package com.trm.roadmate_backend.controller;

import com.trm.roadmate_backend.dto.PathRequest;
import com.trm.roadmate_backend.dto.PathResponse;
import com.trm.roadmate_backend.service.PathFindingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/path")
@RequiredArgsConstructor
public class PathController {

    private final PathFindingService pathFindingService;

    @PostMapping("/shortest")
    public ResponseEntity<PathResponse> findShortestPath(@RequestBody PathRequest request) {
        try {
            PathResponse response = pathFindingService.findShortestPath(
                    request.getStartLat(),
                    request.getStartLng(),
                    request.getEndLat(),
                    request.getEndLng()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/shortest")
    public ResponseEntity<PathResponse> findShortestPathGet(
            @RequestParam Double startLat,
            @RequestParam Double startLng,
            @RequestParam Double endLat,
            @RequestParam Double endLng) {
        try {
            PathResponse response = pathFindingService.findShortestPath(
                    startLat, startLng, endLat, endLng
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}