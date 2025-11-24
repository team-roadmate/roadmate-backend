package com.trm.roadmate_backend.dto;

import lombok.Data;

/**
 * 날씨 응답 DTO
 */
@Data
public class WeatherResponse {

    // 위치 정보
    private double latitude;           // 위도
    private double longitude;          // 경도
    private int gridX;                 // 격자 X
    private int gridY;                 // 격자 Y

    // 초단기실황 데이터
    private String temperature;        // 기온 (℃)
    private String humidity;           // 습도 (%)
    private String rainfall;           // 1시간 강수량 (mm)
    private String precipitationType;  // 강수형태 (없음, 비, 눈 등)
    private String windSpeed;          // 풍속 (m/s)
    private String windDirection;      // 풍향 (deg)

    // 초단기예보 데이터
    private String skyCondition;       // 하늘상태 (맑음, 구름많음, 흐림)

    // 추가 정보
    private String description;        // 날씨 설명 (선택)

    /**
     * 프론트엔드 친화적 날씨 요약 정보
     */
    public String getWeatherSummary() {
        StringBuilder summary = new StringBuilder();

        // 하늘 상태
        summary.append(skyCondition);

        // 강수 정보
        if (!"없음".equals(precipitationType)) {
            summary.append(", ").append(precipitationType);
        }

        return summary.toString();
    }

    /**
     * 날씨 아이콘 추천 (프론트엔드용)
     */
    public String getWeatherIcon() {
        // 강수가 있으면 우선
        if (!"없음".equals(precipitationType)) {
            if (precipitationType.contains("눈")) {
                return "snow";
            } else if (precipitationType.contains("비")) {
                return "rain";
            }
        }

        // 하늘 상태
        return switch (skyCondition) {
            case "맑음" -> "sunny";
            case "구름많음" -> "cloudy";
            case "흐림" -> "overcast";
            default -> "unknown";
        };
    }
}