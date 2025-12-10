package com.trm.roadmate_backend.controller;

import com.trm.roadmate_backend.dto.WeatherResponse;
import com.trm.roadmate_backend.service.WeatherService;

import io.swagger.v3.oas.annotations.Operation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 날씨 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // CORS 설정 (프론트엔드 연동용)
public class WeatherController {

    private final WeatherService weatherService;

    @Operation(
            summary = "현재 날씨 조회",
            description = "위도(lat)와 경도(lon)를 입력받아 해당 위치의 현재 날씨 데이터를 반환합니다."
    )
    @GetMapping("/current")
    public ResponseEntity<WeatherResponse> getCurrentWeather(
            @RequestParam double lat,
            @RequestParam double lon) {

        log.info("날씨 조회 요청 - 위도: {}, 경도: {}", lat, lon);

        WeatherResponse response = weatherService.getCurrentWeather(lat, lon);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "날씨 API 헬스체크",
            description = "Weather API가 정상 작동 중인지 확인합니다."
    )
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
