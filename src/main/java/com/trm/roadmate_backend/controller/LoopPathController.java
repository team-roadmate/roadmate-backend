package com.trm.roadmate_backend.controller;

import com.trm.roadmate_backend.dto.*;
import com.trm.roadmate_backend.service.LoopPathService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/walk")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Loop Path API", description = "루프 산책 경로 탐색 API")
public class LoopPathController {

    private final LoopPathService loopPathService;

    /**
     * 1차 계산: 루프 경로 가능 여부 및 권장 거리 확인
     */
    @PostMapping("/estimate")
    @Operation(summary = "루프 경로 예상 계산",
            description = "출발지와 경유지를 기준으로 최소/권장 루프 거리를 계산합니다")
    public ResponseEntity<LoopEstimateResponse> estimateLoop(
            @RequestBody LoopEstimateRequest request
    ) {
        log.info("Loop estimate requested: start({}, {}), via({}, {})",
                request.getStartLat(), request.getStartLng(),
                request.getViaLat(), request.getViaLng());

        LoopEstimateResponse response = loopPathService.estimateLoop(request);

        return ResponseEntity.ok(response);
    }

    /**
     * 실제 루프 경로 생성
     */
    @PostMapping("/loop")
    @Operation(summary = "루프 산책 경로 생성",
            description = "사용자가 지정한 목표 거리와 형태로 루프 경로를 생성합니다")
    public ResponseEntity<LoopPathResponse> generateLoop(
            @RequestBody LoopPathRequest request
    ) {
        log.info("Loop path requested: start({}, {}), via({}, {}), target={}km, tolerance={}%",
                request.getStartLat(), request.getStartLng(),
                request.getViaLat(), request.getViaLng(),
                request.getTargetDistanceKm(), request.getTolerancePercent());

        // 입력 검증
        if (request.getTargetDistanceKm() == null || request.getTargetDistanceKm() <= 0) {
            return ResponseEntity.badRequest()
                    .body(LoopPathResponse.builder()
                            .message("목표 거리는 0보다 커야 합니다")
                            .withinTolerance(false)
                            .build());
        }

        if (request.getTolerancePercent() == null || request.getTolerancePercent() < 0) {
            request.setTolerancePercent(15); // 기본값
        }

        LoopPathResponse response = loopPathService.generateLoopPath(request);

        return ResponseEntity.ok(response);
    }
}