package com.trm.roadmate_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "walking_node")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalkingNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id", nullable = false, unique = true, length = 100)
    private String nodeId;

    @Column(name = "node_code", nullable = false, length = 10)
    private String nodeCode;

    @Column(name = "sgg_nm", nullable = false, length = 50)
    private String sggNm;

    @Column(name = "node_wkt", columnDefinition = "TEXT")
    private String nodeWkt;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
