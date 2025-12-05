package com.trm.roadmate_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "walking_link")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalkingLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "link_id", nullable = false, unique = true, length = 100)
    private String linkId;

    @Column(name = "link_code", nullable = false, length = 10)
    private String linkCode;

    @Column(name = "start_node_id", length = 100)
    private String startNodeId;

    @Column(name = "end_node_id", length = 100)
    private String endNodeId;

    @Column(name = "sgg_nm", nullable = false, length = 50)
    private String sggNm;

    @Column(name = "link_wkt", columnDefinition = "TEXT")
    private String linkWkt;

    @Column(name = "link_length", precision = 10, scale = 2)
    private BigDecimal linkLength;

    @Column(name = "is_pedestrian")
    private Boolean isPedestrian;

    @Column(name = "is_vehicle")
    private Boolean isVehicle;

    @Column(name = "is_bicycle")
    private Boolean isBicycle;

    @Column(name = "is_pm")
    private Boolean isPm;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        parseLinkCode();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private void parseLinkCode() {
        if (linkCode != null && linkCode.length() == 4) {
            isPedestrian = linkCode.charAt(0) == '1';
            isVehicle = linkCode.charAt(1) == '1';
            isBicycle = linkCode.charAt(2) == '1';
            isPm = linkCode.charAt(3) == '1';
        }
    }
}
