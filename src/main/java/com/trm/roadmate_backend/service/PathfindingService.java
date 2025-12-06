package com.trm.roadmate_backend.service;

import com.trm.roadmate_backend.dto.PathNode;
import com.trm.roadmate_backend.dto.PathResult;
import com.trm.roadmate_backend.entity.Node;
import com.trm.roadmate_backend.service.GraphService.Edge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PathfindingService {

    private final GraphService graphService;

    // --- State 내부 클래스: 다익스트라 우선순위 큐에서 사용 ---
    private static class State implements Comparable<State> {
        public final String nodeId;
        public final double distance;

        public State(String nodeId, double distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }

        @Override
        public int compareTo(State other) {
            return Double.compare(this.distance, other.distance);
        }
    }
    // --------------------------------------------------------

    /**
     * 위도/경도를 입력받아 최단 경로를 탐색하고 좌표를 포함한 PathResult를 반환합니다.
     */
    public PathResult findShortestPathByCoords(
            double startLat, double startLon, double endLat, double endLon)
    {
        // ... 기존 findNearestNodeId 로직 (생략) ...
        String startNodeId = findNearestNodeId(startLat, startLon);
        String endNodeId = findNearestNodeId(endLat, endLon);

        if (startNodeId == null || endNodeId == null) {
            return new PathResult(0.0, Collections.emptyList());
        }

        // Step 2: 다익스트라 로직 호출
        return findShortestPath(startNodeId, endNodeId);
    }

    /**
     * 다익스트라 알고리즘을 사용하여 최단 경로를 탐색하고 좌표를 포함한 PathResult를 반환합니다.
     * (반환 타입이 PathResult로 변경됨)
     */
    public PathResult findShortestPath(String startNodeId, String endNodeId) {
        if (graphService.getNode(startNodeId) == null || graphService.getNode(endNodeId) == null) {
            return new PathResult(0.0, Collections.emptyList());
        }

        Map<String, Double> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        PriorityQueue<State> pq = new PriorityQueue<>();

        distances.put(startNodeId, 0.0);
        pq.add(new State(startNodeId, 0.0));

        while (!pq.isEmpty()) {
            State currentState = pq.poll();
            String currentNodeId = currentState.nodeId;
            double currentDistance = currentState.distance;

            if (currentDistance > distances.getOrDefault(currentNodeId, Double.MAX_VALUE)) {
                continue;
            }

            if (currentNodeId.equals(endNodeId)) {

                // 1. 최종 거리 (currentDistance) 확보
                double rawDistance = currentDistance;

                // 2. 소수점 둘째 자리에서 반올림 처리 (예: 150.7853 -> 150.79)
                double roundedDistance = Math.round(rawDistance * 100.0) / 100.0; // 💡 소수점 둘째 자리까지 유지

                // 3. 경로 복원 및 좌표 변환
                List<String> nodeIdPath = reconstructPath(startNodeId, endNodeId, predecessors);
                List<PathNode> pathWithCoords = convertNodesToPath(nodeIdPath);

                // 4. 반올림된 값으로 PathResult 반환
                return new PathResult(roundedDistance, pathWithCoords);
            }

            List<Edge> edges = graphService.getEdges(currentNodeId);
            for (Edge edge : edges) {
                String nextNodeId = edge.destinationId;
                double weight = edge.weight;
                double newDistance = currentDistance + weight;

                if (newDistance < distances.getOrDefault(nextNodeId, Double.MAX_VALUE)) {
                    distances.put(nextNodeId, newDistance);
                    predecessors.put(nextNodeId, currentNodeId);
                    pq.add(new State(nextNodeId, newDistance));
                }
            }
        }

        log.warn("Path not found from {} to {}", startNodeId, endNodeId);
        return new PathResult(0.0, Collections.emptyList());
    }

    /**
     * 노드 ID 목록을 GraphService를 통해 Node 객체로 변환하고 PathNode 목록을 생성합니다.
     */
    private List<PathNode> convertNodesToPath(List<String> nodeIdPath) {
        List<PathNode> pathNodes = new ArrayList<>();
        for (String nodeId : nodeIdPath) {
            Node node = graphService.getNode(nodeId);
            if (node != null) {
                PathNode pathNode = new PathNode(
                        // 💡 nodeId 필드 제거
                        node.getLatitude(),
                        node.getLongitude()
                );
                pathNodes.add(pathNode);
            } else {
                log.error("Missing Node data for ID: {}", nodeId);
                return Collections.emptyList();
            }
        }
        return pathNodes;
    }

    private List<String> reconstructPath(String startNodeId, String endNodeId, Map<String, String> predecessors) {
        LinkedList<String> path = new LinkedList<>();
        String current = endNodeId;

        while (current != null && !current.equals(startNodeId)) {
            path.addFirst(current);
            current = predecessors.get(current);
        }

        if (current != null && current.equals(startNodeId)) {
            path.addFirst(startNodeId);
        } else {
            return Collections.emptyList();
        }
        return path;
    }

    /**
     * 주어진 좌표에 가장 가까운 Node ID를 찾습니다. (하버사인 공식 사용)
     */
    private String findNearestNodeId(double targetLat, double targetLon) {
        String nearestId = null;
        double minDistance = Double.MAX_VALUE;

        // 경고: 노드 개수가 많으면 성능 문제가 발생합니다.
        for (Node node : graphService.getAllNodes()) {
            double distance = calculateHaversineDistance(
                    targetLat, targetLon, node.getLatitude(), node.getLongitude());

            if (distance < minDistance) {
                minDistance = distance;
                nearestId = node.getNodeId();
            }
        }
        return nearestId;
    }

    /**
     * 두 경위도 좌표 간의 거리를 미터(m) 단위로 계산합니다. (하버사인 공식)
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // 지구 반지름 (미터)
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // 미터 단위 거리 반환
    }
}