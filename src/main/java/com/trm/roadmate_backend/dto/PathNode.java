package com.trm.roadmate_backend.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * 경로를 구성하는 개별 노드의 좌표 정보 (NodeId 제거)
 */
@Getter
@RequiredArgsConstructor
@ToString
public class PathNode {
    // 💡 nodeId 필드 제거
    private final double latitude;
    private final double longitude;

    // PathNode.java는 이대로 사용합니다.
}