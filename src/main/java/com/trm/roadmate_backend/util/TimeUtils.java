package com.trm.roadmate_backend.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 기상청 API Base Time 계산 유틸리티
 */
public class TimeUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");

    /**
     * 초단기실황 Base Date 계산
     * - 매시간 정시에 생성
     * - 10분 이후부터 조회 가능
     */
    public static String getNcstBaseDate(LocalDateTime now) {
        // 현재 시간이 10분 이전이면 전 시간 데이터 사용
        if (now.getMinute() < 10) {
            now = now.minusHours(1);
        }
        return now.format(DATE_FORMATTER);
    }

    /**
     * 초단기실황 Base Time 계산
     */
    public static String getNcstBaseTime(LocalDateTime now) {
        // 현재 시간이 10분 이전이면 전 시간 정시 사용
        if (now.getMinute() < 10) {
            now = now.minusHours(1);
        }
        return String.format("%02d00", now.getHour());
    }

    /**
     * 초단기예보 Base Date 계산
     * - 매시간 30분에 생성
     * - 45분 이후부터 조회 가능
     */
    public static String getFcstBaseDate(LocalDateTime now) {
        // 현재 시간이 45분 이전이면 전 시간 데이터 사용
        if (now.getMinute() < 45) {
            now = now.minusHours(1);
        }
        return now.format(DATE_FORMATTER);
    }

    /**
     * 초단기예보 Base Time 계산
     * - 30분 단위 (0030, 0130, 0230, ...)
     */
    public static String getFcstBaseTime(LocalDateTime now) {
        // 현재 시간이 45분 이전이면 전 시간 30분 사용
        if (now.getMinute() < 45) {
            now = now.minusHours(1);
        }
        return String.format("%02d30", now.getHour());
    }

    /**
     * 단기예보 Base Date 계산 (참고용)
     * - 하루 8회 발표: 02, 05, 08, 11, 14, 17, 20, 23시
     * - 발표 후 10분 이후부터 조회 가능
     */
    public static String getVilageFcstBaseDate(LocalDateTime now) {
        int hour = now.getHour();
        int minute = now.getMinute();

        // 발표 시각 이전이면 이전 발표 시각 사용
        int[] baseHours = {2, 5, 8, 11, 14, 17, 20, 23};
        int baseHour = 23; // 기본값은 전날 23시

        for (int bh : baseHours) {
            if (hour > bh || (hour == bh && minute >= 10)) {
                baseHour = bh;
            }
        }

        // 만약 현재 시각이 02:10 이전이면 전날 23시 사용
        if (hour < 2 || (hour == 2 && minute < 10)) {
            return now.minusDays(1).format(DATE_FORMATTER);
        }

        return now.format(DATE_FORMATTER);
    }

    /**
     * 단기예보 Base Time 계산 (참고용)
     */
    public static String getVilageFcstBaseTime(LocalDateTime now) {
        int hour = now.getHour();
        int minute = now.getMinute();

        int[] baseHours = {2, 5, 8, 11, 14, 17, 20, 23};
        int baseHour = 23;

        for (int bh : baseHours) {
            if (hour > bh || (hour == bh && minute >= 10)) {
                baseHour = bh;
            }
        }

        return String.format("%02d00", baseHour);
    }
}