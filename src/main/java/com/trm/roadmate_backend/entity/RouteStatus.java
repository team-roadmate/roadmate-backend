package com.trm.roadmate_backend.entity;

// WalkRoute 엔티티의 상태를 명확히 관리하기 위한 Enum
public enum RouteStatus {
    STARTED,   // 산책 안내가 시작되었을 때
    COMPLETED, // 산책이 성공적으로 완료되었을 때
    CANCELED   // 산책이 도중에 취소되었을 때
}