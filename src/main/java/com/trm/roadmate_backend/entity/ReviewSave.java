package com.trm.roadmate_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

    @Entity
    @Table(name = "review")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class ReviewSave {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "review_id")
        private Long reviewId;

        // user_id FK (User 엔티티까지 연결하고 싶으면 Long 말고 User로 바꿔도 됨)
        @Column(name = "user_id", nullable = false)
        private Long userId;

        // route_id FK
        @Column(name = "route_id", nullable = false)
        private Long routeId;

        @Column(nullable = false)
        private Integer rating;      // 1~5 점 같은 평점

        @Lob
        @Column(columnDefinition = "TEXT")
        private String comment;

        @Column(name = "image_url", length = 500)
        private String imageUrl;

        @Column(name = "created_at", nullable = false)
        private LocalDateTime createdAt;
    }

