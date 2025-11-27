package com.trm.roadmate_backend.dto.walk;

import lombok.*;

// ===== 구역 정보 응답 DTO =====
@Data
@Builder
public class DistrictInfo {
    private String districtName;
    private Integer nodeCount;
    private Integer linkCount;
    private String collectedAt;
}
