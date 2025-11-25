package com.trm.roadmate_backend.entity.walk;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "walking_links")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalkingLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String linkId;

    @Column(nullable = false, length = 50)
    private String startNodeId;

    @Column(nullable = false, length = 50)
    private String endNodeId;

    @Column(nullable = false)
    private Double distance;

    @Column(nullable = false, length = 50)
    private String district;

    // API 플래그
    @Column(nullable = false)
    private Boolean isElevatedRoad = false;

    @Column(nullable = false)
    private Boolean isSubwayNetwork = false;

    @Column(nullable = false)
    private Boolean isBridge = false;

    @Column(nullable = false)
    private Boolean isTunnel = false;

    @Column(nullable = false)
    private Boolean isOverpass = false;

    @Column(nullable = false)
    private Boolean isCrosswalk = false;

    @Column(nullable = false)
    private Boolean isPark = false;

    @Column(nullable = false)
    private Boolean isBuilding = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
