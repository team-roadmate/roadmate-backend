package com.trm.roadmate_backend.controller;

import com.trm.roadmate_backend.dto.walk.*;
import com.trm.roadmate_backend.service.walk.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 관리자 API - 데이터 수집
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final DataCollectionService dataCollectionService;
    private final GraphService graphService;

    /**
     * 특정 구역 데이터 수집
     * POST /api/admin/collect?district=구로구
     */
    @PostMapping("/collect")
    public ResponseEntity<CollectionResponse> collectDistrict(
            @RequestParam String district) {
        log.info("데이터 수집 요청: {}", district);

        CollectionResponse response = dataCollectionService.collectDistrict(district);

        // 수집 성공 시 그래프 재구축
        if (Boolean.TRUE.equals(response.getSuccess())) {
            graphService.rebuildGraph();
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 수집된 구역 목록 조회
     * GET /api/admin/districts
     */
    @GetMapping("/districts")
    public ResponseEntity<List<DistrictInfo>> getCollectedDistricts() {
        List<DistrictInfo> districts = dataCollectionService.getCollectedDistricts();
        return ResponseEntity.ok(districts);
    }

    /**
     * 그래프 재구축
     * POST /api/admin/rebuild-graph
     */
    @PostMapping("/rebuild-graph")
    public ResponseEntity<String> rebuildGraph() {
        log.info("그래프 재구축 요청");
        graphService.rebuildGraph();
        return ResponseEntity.ok("그래프 재구축 완료");
    }

    /**
     * 그래프 통계
     * GET /api/admin/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<GraphService.GraphStats> getGraphStats() {
        return ResponseEntity.ok(graphService.getStats());
    }
}
