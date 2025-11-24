package com.trm.roadmate_backend.controller;

import com.trm.roadmate_backend.dto.WeatherResponse;
import com.trm.roadmate_backend.service.WeatherService;
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

    /**
     * 현재 날씨 조회
     *
     * @param lat 위도
     * @param lon 경도
     * @return 날씨 정보
     */
    @GetMapping("/current")
    public ResponseEntity<WeatherResponse> getCurrentWeather(
            @RequestParam double lat,
            @RequestParam double lon) {

        log.info("날씨 조회 요청 - 위도: {}, 경도: {}", lat, lon);

        WeatherResponse response = weatherService.getCurrentWeather(lat, lon);

        return ResponseEntity.ok(response);
    }

    /**
     * 헬스체크용 엔드포인트
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}