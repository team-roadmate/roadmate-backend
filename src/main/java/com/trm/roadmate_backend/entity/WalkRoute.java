package com.trm.roadmate_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "walk_route")
public class WalkRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_id")
    private Long routeId;

    @Column(name = "user_id", nullable = false)
    private Integer userId; // User 엔티티의 PK와 타입 일치

    // 메타데이터
    @Column(name = "title", length = 50)
    private String title;

    @Column(name = "memo", length = 255)
    private String memo;

    @Column(name = "rating")
    private Integer rating;

    // 시간 및 거리 기록
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    // 예상 데이터 (START 시 기록)
    @Column(name = "expected_distance", nullable = false)
    private Float expectedDistance;

    @Column(name = "expected_duration", nullable = false)
    private Integer expectedDuration;

    // 실제 데이터 (COMPLETE 시 기록)
    @Column(name = "distance")
    private Float distance;

    @Column(name = "duration")
    private Integer duration;

    // 상태 및 플래그
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private RouteStatus status;

    @Builder.Default // ✨ Builder 사용 시 기본값 강제 적용
    @Column(name = "is_course", nullable = false)
    private Boolean isCourse = false;

    @Builder.Default // ✨ Builder 사용 시 기본값 강제 적용
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // 경로 좌표 데이터
    @Column(name = "path_data", columnDefinition = "JSON", nullable = false)
    private String pathData; // JSON String으로 저장

    // --- 비즈니스 메서드 ---

    public void complete(Float distance, Integer duration) {
        this.status = RouteStatus.COMPLETED;
        this.distance = distance;
        this.duration = duration;
        this.endTime = LocalDateTime.now();
    }

    public void setAsCourse(String title, String memo, Integer rating) {
        this.isCourse = true;
        this.title = title;
        this.memo = memo;
        this.rating = rating;
    }

    public void unSetAsCourse() {
        this.isCourse = false;
        this.title = null;
        this.memo = null;
        this.rating = null;
    }

    public void delete() {
        this.isDeleted = true;
    }
}