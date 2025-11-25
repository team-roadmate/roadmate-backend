package com.trm.roadmate_backend.dto.walk;

import lombok.*;

// ===== 데이터 수집 응답 DTO =====
@Data
@Builder
public class CollectionResponse {
    private String district;
    private Integer nodeCount;
    private Integer linkCount;
    private String message;
    private Boolean success;
}
