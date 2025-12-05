package com.trm.roadmate_backend.controller;

import com.trm.roadmate_backend.entity.ApiCallHistory;
import com.trm.roadmate_backend.service.WalkingNetworkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/walking-network")
@RequiredArgsConstructor
public class WalkingNetworkController {

    private final WalkingNetworkService walkingNetworkService;

    @PostMapping("/fetch/{sggNm}")
    public ResponseEntity<Map<String, Object>> fetchWalkingNetwork(@PathVariable String sggNm) {
        log.info("도보 네트워크 데이터 수집 요청: {}", sggNm);

        try {
            ApiCallHistory history = walkingNetworkService.fetchAndSaveWalkingNetwork(sggNm);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sggNm", sggNm);
            response.put("status", history.getStatus());
            response.put("totalCount", history.getTotalCount());
            response.put("successCount", history.getSuccessCount());
            response.put("failCount", history.getFailCount());
            response.put("startTime", history.getStartTime());
            response.put("endTime", history.getEndTime());

            if (history.getErrorMessage() != null) {
                response.put("errorMessage", history.getErrorMessage());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("데이터 수집 중 오류", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/stats/{sggNm}")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable String sggNm) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("sggNm", sggNm);
        stats.put("nodeCount", walkingNetworkService.getNodeCount(sggNm));
        stats.put("linkCount", walkingNetworkService.getLinkCount(sggNm));

        return ResponseEntity.ok(stats);
    }
}
