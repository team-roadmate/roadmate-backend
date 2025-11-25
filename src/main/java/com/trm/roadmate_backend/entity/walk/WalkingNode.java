package com.trm.roadmate_backend.entity.walk;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// ===== 노드 엔티티 =====
@Entity
@Table(name = "walking_nodes")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalkingNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String nodeId;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 100)
    private String nodeType;

    @Column(nullable = false, length = 50)
    private String district;

    @Column(length = 50)
    private String neighborhood;

    // API 플래그
    @Column(nullable = false)
    private Boolean isCrosswalk = false;

    @Column(nullable = false)
    private Boolean isOverpass = false;

    @Column(nullable = false)
    private Boolean isBridge = false;

    @Column(nullable = false)
    private Boolean isTunnel = false;

    @Column(nullable = false)
    private Boolean isSubway = false;

    @Column(nullable = false)
    private Boolean isPark = false;

    @Column(nullable = false)
    private Boolean isBuilding = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
