package com.trm.roadmate_backend.entity.walk;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "collected_districts")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectedDistrict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String districtName;

    @Column(nullable = false)
    private Integer nodeCount = 0;

    @Column(nullable = false)
    private Integer linkCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime collectedAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
