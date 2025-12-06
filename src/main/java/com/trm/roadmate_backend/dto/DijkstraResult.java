package com.trm.roadmate_backend.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * 다익스트라 경로 탐색 결과 DTO
 */
@Getter
@RequiredArgsConstructor
@ToString
public class DijkstraResult {
    private final double totalDistance; // 총 거리 (미터)
    private final List<String> nodeIdPath; // 순서대로 정렬된 노드 ID 목록
}