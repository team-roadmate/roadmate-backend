package com.trm.roadmate_backend.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * 최종 경로 탐색 결과 DTO
 */
@Getter
@RequiredArgsConstructor
@ToString
public class PathResult {
    private final double totalDistance; // 총 거리 (미터)
    private final List<PathNode> path; // 순서대로 정렬된 PathNode 목록
}