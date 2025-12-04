package com.trm.roadmate_backend.service.walk;

import com.trm.roadmate_backend.dto.walk.*;
import com.trm.roadmate_backend.service.walk.GraphService.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PathFindingService {

    private final GraphService graphService;

    /**
     * 경로 탐색 메인 메서드
     */
    public PathResponse findPath(PathRequest request) {
        log.info("경로 탐색 요청: ({}, {}) → ({}, {})",
                request.getStartLat(), request.getStartLng(),
                request.getEndLat(), request.getEndLng());

        try {
            if (graphService.isEmpty()) {
                return PathResponse.builder()
                        .message("그래프 데이터가 없습니다. 먼저 지역 데이터를 수집하세요.")
                        .build();
            }

            // 2. 시작/끝 좌표에서 가장 가까운 노드 찾기
            String startNodeId = findNearestNode(request.getStartLat(), request.getStartLng());
            String endNodeId = findNearestNode(request.getEndLat(), request.getEndLng());

            if (startNodeId == null || endNodeId == null) {
                return PathResponse.builder()
                        .message("근처에 노드를 찾을 수 없습니다. 다른 위치를 시도하세요.")
                        .build();
            }

            log.info("시작 노드: {}, 종료 노드: {}", startNodeId, endNodeId);
            log.info("경로 탐색 시작: ({}) → ({}) (Mode: Dijkstra)", startNodeId, endNodeId);


            // 3. 다익스트라 알고리즘으로 경로 탐색
            DijkstraResult result = findDijkstraPath(startNodeId, endNodeId);

            if (result.pathNodeIds.isEmpty()) {
                return PathResponse.builder()
                        .message("경로를 찾을 수 없습니다.")
                        .build();
            }

            // 4. 노드 ID → 좌표 변환
            List<Coordinate> coordinates = result.pathNodeIds.stream()
                    .map(graphService::getNode)
                    .filter(Objects::nonNull)
                    .map(node -> new Coordinate(node.getLat(), node.getLng()))
                    .toList();

            // 5. 총 거리 계산 (다익스트라가 계산한 최종 최단 거리)
            double totalDistance = result.getTotalDistance();

            // 6. 예상 시간 계산 (1.2m/s 보행 속도)
            int duration = (int) (totalDistance / 1.2);

            log.info("경로 탐색 완료: 거리 {}m, 시간 {}초, 노드 {}개",
                    totalDistance, duration, coordinates.size());

            return PathResponse.builder()
                    .path(coordinates)
                    .distance(totalDistance)
                    .duration(duration)
                    .message("경로 탐색 성공")
                    .build();

        } catch (Exception e) {
            log.error("경로 탐색 실패: {}", e.getMessage(), e);
            return PathResponse.builder()
                    .message("경로 탐색 중 오류 발생: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 다익스트라 알고리즘 실행
     */
    private DijkstraResult findDijkstraPath(String start, String end) {
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>();

        // 초기화
        dist.put(start, 0.0);
        pq.offer(new NodeDistance(start, 0.0));

        // 🔍 추가: 시작 노드의 연결 상태 확인
        List<Edge> startEdges = graphService.getEdges(start);
        log.info("시작 노드 {} 연결 개수: {}", start, startEdges.size());
        if (!startEdges.isEmpty()) {
            log.info("시작 노드 첫 3개 연결: {}",
                    startEdges.stream().limit(3).map(Edge::getTargetNodeId).toList());
        }

        List<Edge> endEdges = graphService.getEdges(end);
        log.info("종료 노드 {} 연결 개수: {}", end, endEdges.size());

        int visitedCount = 0; // 🔍 추가: 방문한 노드 개수
        int maxQueueSize = 0; // 🔍 추가: 큐 최대 크기

        while (!pq.isEmpty()) {
            maxQueueSize = Math.max(maxQueueSize, pq.size());
            NodeDistance current = pq.poll();
            String currentNodeId = current.nodeId;

            visitedCount++; // 🔍 추가

            // 목적지 도착
            if (currentNodeId.equals(end)) {
                double finalDistance = dist.get(end);
                log.info("✅ 경로 발견! 방문 노드: {}, 최대 큐: {}", visitedCount, maxQueueSize);
                return new DijkstraResult(reconstructPath(previous, start, end), finalDistance);
            }

            // 이미 더 짧은 경로가 발견된 경우 스킵
            if (current.getDistance() > dist.getOrDefault(currentNodeId, Double.MAX_VALUE)) {
                continue;
            }

            // 인접 노드 탐색
            List<Edge> edges = graphService.getEdges(currentNodeId);
            for (Edge edge : edges) {
                String nextNodeId = edge.getTargetNodeId();
                double weight = edge.getDistance();

                double newDist = dist.get(currentNodeId) + weight;

                if (newDist < dist.getOrDefault(nextNodeId, Double.MAX_VALUE)) {
                    dist.put(nextNodeId, newDist);
                    previous.put(nextNodeId, currentNodeId);
                    pq.offer(new NodeDistance(nextNodeId, newDist));
                }
            }
        }

        // 경로를 찾지 못한 경우
        log.error("❌ Dijkstra 탐색 실패: 방문 노드 {}, 최대 큐 {}, 탐색한 고유 노드 {}",
                visitedCount, maxQueueSize, dist.size());
        return new DijkstraResult(Collections.emptyList(), 0.0);
    }

    /**
     * 경로 재구성
     */
    private List<String> reconstructPath(Map<String, String> previous, String start, String end) {
        List<String> path = new ArrayList<>();
        String current = end;

        while (current != null) {
            path.add(current);
            if (current.equals(start)) {
                break;
            }
            current = previous.get(current);
        }

        if (path.isEmpty() || !path.getLast().equals(start)) {
            return Collections.emptyList();
        }

        Collections.reverse(path);
        return path;
    }

    /**
     * 가장 가까운 노드 찾기
     */
    private String findNearestNode(double lat, double lng) {
        return graphService.getAllNodes().stream()
                .min(Comparator.comparingDouble(node ->
                        calculateDistance(lat, lng, node.getLat(), node.getLng())
                ))
                .map(Node::getId)
                .orElse(null);
    }

    /**
     * 하버사인 거리 공식 (미터)
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371000; // 지구 반지름 (미터)

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    // ===== 내부 클래스 =====

    @Data
    private static class DijkstraResult {
        List<String> pathNodeIds;
        double totalDistance;

        public DijkstraResult(List<String> pathNodeIds, double totalDistance) {
            this.pathNodeIds = pathNodeIds;
            this.totalDistance = totalDistance;
        }
    }

    @Data
    @AllArgsConstructor
    private static class NodeDistance implements Comparable<NodeDistance> {
        private String nodeId;
        private Double distance;

        @Override
        public int compareTo(NodeDistance other) {
            // 1. 거리가 짧은 순서로 정렬
            int distComparison = Double.compare(this.getDistance(), other.getDistance());
            if (distComparison != 0) {
                return distComparison;
            }
            // 💡 [핵심 수정] 거리가 같을 경우, nodeId를 기준으로 비교하여 PriorityQueue 안정성 확보
            return this.nodeId.compareTo(other.nodeId);
        }
    }
}