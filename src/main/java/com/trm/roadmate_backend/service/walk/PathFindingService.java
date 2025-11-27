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
        log.info("경로 탐색 시작: ({}, {}) → ({}, {})",
                request.getStartLat(), request.getStartLng(),
                request.getEndLat(), request.getEndLng());

        try {
            // 1. 그래프 확인
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

            // 3. 다익스트라 알고리즘으로 경로 탐색
            List<String> pathNodeIds = dijkstra(startNodeId, endNodeId, request);

            if (pathNodeIds.isEmpty()) {
                return PathResponse.builder()
                        .message("경로를 찾을 수 없습니다.")
                        .build();
            }

            // 4. 노드 ID → 좌표 변환
            List<Coordinate> coordinates = pathNodeIds.stream()
                    .map(nodeId -> graphService.getNode(nodeId))
                    .filter(Objects::nonNull)
                    .map(node -> new Coordinate(node.getLat(), node.getLng()))
                    .toList();

            // 5. 총 거리 계산
            double totalDistance = calculateTotalDistance(pathNodeIds);

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
     * 다익스트라 알고리즘
     */
    private List<String> dijkstra(String start, String end, PathRequest request) {
        Map<String, Double> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>();
        Set<String> visited = new HashSet<>();

        // 초기화
        distances.put(start, 0.0);
        pq.offer(new NodeDistance(start, 0.0));

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            String currentNode = current.nodeId;

            // 이미 방문한 노드는 스킵
            if (visited.contains(currentNode)) {
                continue;
            }
            visited.add(currentNode);

            // 목적지 도착
            if (currentNode.equals(end)) {
                break;
            }

            // 인접 노드 탐색
            List<Edge> edges = graphService.getEdges(currentNode);
            for (Edge edge : edges) {
                String nextNode = edge.getTargetNodeId();

                if (visited.contains(nextNode)) {
                    continue;
                }

                // 가중치 계산 (옵션 적용)
                double weight = calculateWeight(edge, request);
                double newDist = distances.get(currentNode) + weight;

                if (newDist < distances.getOrDefault(nextNode, Double.MAX_VALUE)) {
                    distances.put(nextNode, newDist);
                    previous.put(nextNode, currentNode);
                    pq.offer(new NodeDistance(nextNode, newDist));
                }
            }
        }

        // 경로 재구성
        return reconstructPath(previous, start, end);
    }

    /**
     * 가중치 계산 (사용자 옵션 적용)
     */
    private double calculateWeight(Edge edge, PathRequest request) {
        double weight = edge.getDistance(); // 기본은 거리

        // 공원 선호
        if (Boolean.TRUE.equals(request.getPreferPark()) && Boolean.TRUE.equals(edge.getIsPark())) {
            weight *= 0.7; // 30% 할인
        }

        // 육교 피하기
        if (Boolean.TRUE.equals(request.getAvoidOverpass()) && Boolean.TRUE.equals(edge.getIsOverpass())) {
            weight *= 1.5; // 50% 증가
        }

        // 터널 피하기
        if (Boolean.TRUE.equals(request.getAvoidTunnel()) && Boolean.TRUE.equals(edge.getIsTunnel())) {
            weight *= 1.5;
        }

        // 실내 선호 (비 오는 날)
        if (Boolean.TRUE.equals(request.getPreferIndoor()) && Boolean.TRUE.equals(edge.getIsBuilding())) {
            weight *= 0.8;
        }

        return weight;
    }

    /**
     * 경로 재구성
     */
    private List<String> reconstructPath(Map<String, String> previous, String start, String end) {
        List<String> path = new ArrayList<>();
        String current = end;

        // 역순으로 경로 추적
        while (current != null) {
            path.add(current);
            if (current.equals(start)) {
                break;
            }
            current = previous.get(current);
        }

        // 경로가 끊긴 경우
        if (!path.get(path.size() - 1).equals(start)) {
            return Collections.emptyList();
        }

        // 올바른 순서로 뒤집기
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
     * 총 거리 계산
     */
    private double calculateTotalDistance(List<String> pathNodeIds) {
        double total = 0.0;

        for (int i = 0; i < pathNodeIds.size() - 1; i++) {
            String currentId = pathNodeIds.get(i);
            String nextId = pathNodeIds.get(i + 1);

            Node current = graphService.getNode(currentId);
            Node next = graphService.getNode(nextId);

            if (current != null && next != null) {
                total += calculateDistance(
                        current.getLat(), current.getLng(),
                        next.getLat(), next.getLng()
                );
            }
        }

        return total;
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
    @AllArgsConstructor
    private static class NodeDistance implements Comparable<NodeDistance> {
        private String nodeId;
        private Double distance;

        @Override
        public int compareTo(NodeDistance other) {
            return Double.compare(this.distance, other.distance);
        }
    }
}