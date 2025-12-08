package com.trm.roadmate_backend.controller;

import com.trm.roadmate_backend.service.WalkingNetworkService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/walking-network")
@RequiredArgsConstructor
public class WalkingNetworkController {

    private final WalkingNetworkService walkingNetworkService;

    @Operation(
            summary = "구별 도보 네트워크 데이터 가져오기",
            description = "지정한 행정구(districtName)의 도보 네트워크 데이터를 API로부터 가져와 저장합니다."
    )
    @PostMapping("/import/{districtName}")
    public ResponseEntity<String> importDistrict(@PathVariable String districtName) {
        try {
            String result = walkingNetworkService.importDistrictData(districtName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    @Operation(
            summary = "워킹 네트워크 API 상태 확인",
            description = "Walking Network API가 정상 동작 중인지 확인합니다."
    )
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Walking Network API is running");
    }
}
