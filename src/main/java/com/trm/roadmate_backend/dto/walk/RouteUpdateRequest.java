package com.trm.roadmate_backend.dto.walk;

// Lombok @Builder, @Data는 생략하고 단순 Record 형태로 정의합니다.

public record RouteUpdateRequest(
        String title,           // 경로 제목
        String userMemo,        // 사용자 메모
        Integer developerRating, // 사용자 평점
        Boolean isCompleted     // 완주 여부
) {}