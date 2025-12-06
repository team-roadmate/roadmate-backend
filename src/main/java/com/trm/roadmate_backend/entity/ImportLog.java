package com.trm.roadmate_backend.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "import_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImportLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String sggNm; // 시군구 명

    private Integer totalNodes = 0; // 총 노드 수
    private Integer totalLinks = 0; // 총 링크 수
    private Integer virtualNodes = 0; // 생성된 가상 노드 수

    @Column(length = 20)
    private String status; // 상태 (RUNNING, SUCCESS, FAILED)

    private LocalDateTime startedAt; // 시작 시간
    private LocalDateTime completedAt; // 완료 시간

    @Column(columnDefinition = "TEXT")
    private String errorMessage; // 에러 메시지
}