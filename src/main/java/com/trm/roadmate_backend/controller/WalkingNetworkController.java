package com.trm.roadmate_backend.controller;

import com.trm.roadmate_backend.service.WalkingNetworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/walking-network")
@RequiredArgsConstructor
public class WalkingNetworkController {

    private final WalkingNetworkService walkingNetworkService;

    @PostMapping("/import/{districtName}")
    public ResponseEntity<String> importDistrict(@PathVariable String districtName) {
        try {
            String result = walkingNetworkService.importDistrictData(districtName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Walking Network API is running");
    }
}