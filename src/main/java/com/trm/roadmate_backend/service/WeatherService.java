package com.trm.roadmate_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trm.roadmate_backend.dto.WeatherResponse;
import com.trm.roadmate_backend.util.GridConverter;
import com.trm.roadmate_backend.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.base-url}")
    private String baseUrl;

    /**
     * 위도/경도를 받아 현재 날씨 정보를 반환
     */
    public WeatherResponse getCurrentWeather(double lat, double lon) {
        // 1. 위경도 -> 격자 좌표 변환
        int[] grid = GridConverter.toGrid(lat, lon);
        int nx = grid[0];
        int ny = grid[1];

        log.info("위경도 ({}, {}) -> 격자 좌표 ({}, {})", lat, lon, nx, ny);

        // 2. Base Time 계산
        LocalDateTime now = LocalDateTime.now();
        String ncstBaseDate = TimeUtils.getNcstBaseDate(now);
        String ncstBaseTime = TimeUtils.getNcstBaseTime(now);
        String fcstBaseDate = TimeUtils.getFcstBaseDate(now);
        String fcstBaseTime = TimeUtils.getFcstBaseTime(now);

        log.info("초단기실황 - baseDate: {}, baseTime: {}", ncstBaseDate, ncstBaseTime);
        log.info("초단기예보 - baseDate: {}, baseTime: {}", fcstBaseDate, fcstBaseTime);

        // 3. 초단기실황 조회 (온도, 강수형태 등)
        Map<String, String> ncstData = getUltraSrtNcst(ncstBaseDate, ncstBaseTime, nx, ny);

        // 4. 초단기예보 조회 (하늘상태)
        Map<String, String> fcstData = getUltraSrtFcst(fcstBaseDate, fcstBaseTime, nx, ny);

        // 5. 데이터 통합
        return buildWeatherResponse(ncstData, fcstData, lat, lon, nx, ny);
    }

    /**
     * 초단기실황 API 호출
     */
    private Map<String, String> getUltraSrtNcst(String baseDate, String baseTime, int nx, int ny) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/getUltraSrtNcst")
                .queryParam("serviceKey", apiKey)
                .queryParam("numOfRows", 10)
                .queryParam("pageNo", 1)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDate)
                .queryParam("base_time", baseTime)
                .queryParam("nx", nx)
                .queryParam("ny", ny)
                .build(false) // 인코딩 안함 (serviceKey 중복 인코딩 방지)
                .toUriString();

        log.debug("초단기실황 요청 URL: {}", url);

        String response = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        return parseWeatherData(response);
    }

    /**
     * 초단기예보 API 호출
     */
    private Map<String, String> getUltraSrtFcst(String baseDate, String baseTime, int nx, int ny) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/getUltraSrtFcst")
                .queryParam("serviceKey", apiKey)
                .queryParam("numOfRows", 60)
                .queryParam("pageNo", 1)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDate)
                .queryParam("base_time", baseTime)
                .queryParam("nx", nx)
                .queryParam("ny", ny)
                .build(false)
                .toUriString();

        log.debug("초단기예보 요청 URL: {}", url);

        String response = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        return parseWeatherData(response);
    }

    /**
     * API 응답 JSON 파싱
     */
    private Map<String, String> parseWeatherData(String response) {
        Map<String, String> dataMap = new HashMap<>();

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.path("response").path("body").path("items").path("item");

            if (items.isArray()) {
                for (JsonNode item : items) {
                    String category = item.path("category").asText();
                    String value = item.path("fcstValue").asText();

                    // 실황 데이터는 obsrValue, 예보 데이터는 fcstValue
                    if (value.isEmpty()) {
                        value = item.path("obsrValue").asText();
                    }

                    dataMap.put(category, value);
                }
            }

            log.debug("파싱된 데이터: {}", dataMap);
        } catch (Exception e) {
            log.error("응답 파싱 실패: {}", e.getMessage(), e);
            throw new RuntimeException("날씨 데이터 파싱 실패", e);
        }

        return dataMap;
    }

    /**
     * 응답 데이터 생성
     */
    private WeatherResponse buildWeatherResponse(
            Map<String, String> ncstData,
            Map<String, String> fcstData,
            double lat, double lon, int nx, int ny) {

        WeatherResponse response = new WeatherResponse();
        response.setLatitude(lat);
        response.setLongitude(lon);
        response.setGridX(nx);
        response.setGridY(ny);

        // 초단기실황 데이터
        response.setTemperature(ncstData.getOrDefault("T1H", "N/A")); // 기온
        response.setHumidity(ncstData.getOrDefault("REH", "N/A"));    // 습도
        response.setRainfall(ncstData.getOrDefault("RN1", "0"));      // 1시간 강수량
        response.setPrecipitationType(getPrecipitationTypeName(ncstData.getOrDefault("PTY", "0"))); // 강수형태
        response.setWindSpeed(ncstData.getOrDefault("WSD", "N/A"));   // 풍속
        response.setWindDirection(ncstData.getOrDefault("VEC", "N/A")); // 풍향

        // 초단기예보 데이터
        response.setSkyCondition(getSkyConditionName(fcstData.getOrDefault("SKY", "0"))); // 하늘상태

        return response;
    }

    /**
     * 강수형태 코드 -> 이름 변환
     */
    private String getPrecipitationTypeName(String code) {
        return switch (code) {
            case "0" -> "없음";
            case "1" -> "비";
            case "2" -> "비/눈";
            case "3" -> "눈";
            case "5" -> "빗방울";
            case "6" -> "빗방울눈날림";
            case "7" -> "눈날림";
            default -> "알 수 없음";
        };
    }

    /**
     * 하늘상태 코드 -> 이름 변환
     */
    private String getSkyConditionName(String code) {
        return switch (code) {
            case "1" -> "맑음";
            case "3" -> "구름많음";
            case "4" -> "흐림";
            default -> "알 수 없음";
        };
    }
}