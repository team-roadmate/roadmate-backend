package com.trm.roadmate_backend.dto.walk;

import lombok.*;

// ===== 좌표 DTO =====
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Coordinate {
    private Double latitude;
    private Double longitude;
}
