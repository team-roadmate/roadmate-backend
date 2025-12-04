package com.trm.roadmate_backend.entity.walk;

import com.trm.roadmate_backend.dto.walk.RouteUpdateRequest; // 💡 추가
import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "walking_route")
public class WalkingRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    // --- 사용자 기록 필드 ---
    private String title;
    @Column(columnDefinition = "TEXT")
    private String userMemo;
    private Integer developerRating;

    // 💡 핵심: 좌표 리스트 JSON 문자열 (수정 불가 필드)
    @Column(columnDefinition = "TEXT")
    private String pathCoordinatesJson;

    // --- 통계 및 상태 필드 (수정 불가 필드) ---
    private double totalDistance;
    private long durationSeconds;
    private LocalDateTime savedAt;
    private boolean isCompleted;

    /**
     * 경로의 메타데이터 필드만 선택적으로 업데이트합니다.
     */
    public void update(RouteUpdateRequest request) {
        if (request.title() != null) {
            this.title = request.title();
        }
        if (request.userMemo() != null) {
            this.userMemo = request.userMemo();
        }
        if (request.developerRating() != null) {
            this.developerRating = request.developerRating();
        }
        // isCompleted는 boolean 타입이지만, DTO에서는 null 체크를 위해 Boolean 래퍼 클래스 사용
        if (request.isCompleted() != null) {
            this.isCompleted = request.isCompleted();
        }
        // 경로 좌표, 거리, 시간 등은 수정하지 않습니다.
    }
}