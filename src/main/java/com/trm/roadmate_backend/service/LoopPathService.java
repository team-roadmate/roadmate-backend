package com.trm.roadmate_backend.service;

import com.trm.roadmate_backend.dto.*;
import com.trm.roadmate_backend.entity.Node;
import com.trm.roadmate_backend.service.GraphService.Edge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoopPathService {

    private final GraphService graphService;
    private final PathfindingService pathfindingService;

    // ==================== 1차 계산: 루프 가능 여부 확인 ====================
    public LoopEstimateResponse estimateLoop(LoopEstimateRequest request) {
        // Step 1: 직선 거리 계산
        double straightDist = calculateHaversine(
                request.getStartLat(), request.getStartLng(),
                request.getViaLat(), request.getViaLng()
        ) / 1000.0; // km 변환

        // Step 2: 가장 가까운 노드 찾기
        String startNodeId = findNearestNodeId(request.getStartLat(), request.getStartLng());
        String viaNodeId = findNearestNodeId(request.getViaLat(), request.getViaLng());

        if (startNodeId == null || viaNodeId == null) {
            return LoopEstimateResponse.builder()
                    .feasible(false)
                    .message("주변에 보행 경로가 없습니다")
                    .build();
        }

        // 너무 가까운 경우 체크
        if (straightDist < 0.5) {
            return LoopEstimateResponse.builder()
                    .feasible(false)
                    .straightDistance(straightDist)
                    .message("경유지가 너무 가깝습니다. 최소 500m 이상 떨어뜨려주세요")
                    .build();
        }

        // Step 3: S→V 최단 경로
        PathResult path1 = pathfindingService.findShortestPath(startNodeId, viaNodeId);
        double dist1 = path1.getTotalDistance() / 1000.0; // km

        // Step 4: V→S 최단 경로 (왔던 길 회피)
        Set<String> usedLinkIds = extractLinkIds(path1.getPath(), startNodeId, viaNodeId);
        PathResult path2 = findPathWithAvoidance(viaNodeId, startNodeId, usedLinkIds);
        double dist2 = path2.getTotalDistance() / 1000.0; // km

        double minLoop = dist1 + dist2;

        // Step 5: 권장 범위 계산
        double recommendedMin = Math.max(minLoop * 1.1, straightDist * 2.5);
        double recommendedMax = straightDist * 7.0;

        return LoopEstimateResponse.builder()
                .minLoopDistance(Math.round(minLoop * 100.0) / 100.0)
                .straightDistance(Math.round(straightDist * 100.0) / 100.0)
                .recommendedMin(Math.round(recommendedMin * 100.0) / 100.0)
                .recommendedMax(Math.round(recommendedMax * 100.0) / 100.0)
                .feasible(true)
                .message("루프 경로 생성 가능")
                .build();
    }

    // ==================== 실제 루프 경로 생성 ====================
    public LoopPathResponse generateLoopPath(LoopPathRequest request) {
        double targetKm = request.getTargetDistanceKm();
        double tolerancePct = request.getTolerancePercent() / 100.0;

        // Step 1: 노드 찾기
        String startNodeId = findNearestNodeId(request.getStartLat(), request.getStartLng());
        String viaNodeId = findNearestNodeId(request.getViaLat(), request.getViaLng());

        if (startNodeId == null || viaNodeId == null) {
            return LoopPathResponse.builder()
                    .withinTolerance(false)
                    .message("주변에 보행 경로가 없습니다")
                    .build();
        }

        // Step 2: S→V 경로 (목표의 45% 거리 사용)
        double target1 = targetKm * 0.45;
        PathResult path1 = findPathWithTargetDistance(
                startNodeId, viaNodeId, target1,
                Collections.emptySet(), request.getDetourDirection(), true
        );
        double dist1Km = path1.getTotalDistance() / 1000.0;

        // Step 3: V→S 경로 (나머지 거리 사용, 왔던 길 회피)
        Set<String> usedLinks = extractLinkIds(path1.getPath(), startNodeId, viaNodeId);
        double target2 = targetKm - dist1Km;
        PathResult path2 = findPathWithTargetDistance(
                viaNodeId, startNodeId, target2,
                usedLinks, getOppositeDirection(request.getDetourDirection()), false
        );
        double dist2Km = path2.getTotalDistance() / 1000.0;

        // Step 4: 경로 합치기
        List<PathNode> fullPath = new ArrayList<>(path1.getPath());
        if (!path2.getPath().isEmpty()) {
            fullPath.addAll(path2.getPath().subList(1, path2.getPath().size()));
        }

        double actualKm = dist1Km + dist2Km;
        double toleranceKm = Math.abs(actualKm - targetKm);
        boolean withinTolerance = toleranceKm <= (targetKm * tolerancePct);

        return LoopPathResponse.builder()
                .actualDistance(Math.round(actualKm * 100.0) / 100.0)
                .targetDistance(targetKm)
                .tolerance(Math.round(toleranceKm * 100.0) / 100.0)
                .withinTolerance(withinTolerance)
                .path(fullPath)
                .segment1(LoopPathResponse.SegmentInfo.builder()
                        .from("출발지").to("경유지")
                        .distance(Math.round(dist1Km * 100.0) / 100.0)
                        .nodeCount(path1.getPath().size())
                        .build())
                .segment2(LoopPathResponse.SegmentInfo.builder()
                        .from("경유지").to("출발지")
                        .distance(Math.round(dist2Km * 100.0) / 100.0)
                        .nodeCount(path2.getPath().size())
                        .build())
                .message(withinTolerance ? "경로 생성 성공" : "목표 거리 달성 실패 (최선의 경로 반환)")
                .build();
    }

    // ==================== 수정된 Dijkstra (왔던 길 회피 + 목표 거리) ====================
    private PathResult findPathWithTargetDistance(
            String startNodeId, String endNodeId, double targetKm,
            Set<String> avoidLinkIds, String direction, boolean isFirstSegment
    ) {
        if (graphService.getNode(startNodeId) == null || graphService.getNode(endNodeId) == null) {
            return new PathResult(0.0, Collections.emptyList());
        }

        Map<String, Double> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        PriorityQueue<State> pq = new PriorityQueue<>();

        distances.put(startNodeId, 0.0);
        pq.add(new State(startNodeId, 0.0));

        Node endNode = graphService.getNode(endNodeId);
        double targetMeters = targetKm * 1000.0;

        while (!pq.isEmpty()) {
            State current = pq.poll();
            String currentNodeId = current.nodeId;
            double currentDist = current.distance;

            if (currentDist > distances.getOrDefault(currentNodeId, Double.MAX_VALUE)) {
                continue;
            }

            if (currentNodeId.equals(endNodeId)) {
                List<String> nodeIdPath = reconstructPath(startNodeId, endNodeId, predecessors);
                List<PathNode> pathWithCoords = convertToPathNodes(nodeIdPath);
                return new PathResult(currentDist, pathWithCoords);
            }

            List<Edge> edges = graphService.getEdges(currentNodeId);
            for (Edge edge : edges) {
                // 왔던 길 차단
                if (avoidLinkIds.contains(edge.linkId)) {
                    continue;
                }

                String nextNodeId = edge.destinationId;
                double baseCost = edge.weight;

                // 비용 계산
                double cost = calculateModifiedCost(
                        currentNodeId, nextNodeId, baseCost,
                        currentDist, targetMeters, endNode, direction, isFirstSegment
                );

                double newDist = currentDist + cost;

                if (newDist < distances.getOrDefault(nextNodeId, Double.MAX_VALUE)) {
                    distances.put(nextNodeId, newDist);
                    predecessors.put(nextNodeId, currentNodeId);
                    pq.add(new State(nextNodeId, newDist));
                }
            }
        }

        log.warn("Path not found from {} to {}", startNodeId, endNodeId);
        return new PathResult(0.0, Collections.emptyList());
    }

    // ==================== 비용 함수 (목표 거리 + 방향 제어) ====================
    private double calculateModifiedCost(
            String currentId, String nextId, double baseCost,
            double currentDist, double targetDist, Node endNode,
            String direction, boolean isFirstSegment
    ) {
        Node currentNode = graphService.getNode(currentId);
        Node nextNode = graphService.getNode(nextId);

        double cost = baseCost;

        // 1. 목표 거리 유도 (±20% 범위 내에서만 적용)
        double projectedDist = currentDist + baseCost;
        double distGap = Math.abs(projectedDist - targetDist);
        if (distGap < targetDist * 0.2) {
            double penalty = distGap / targetDist;
            cost *= (1 + penalty * 0.5);
        }

        // 2. 방향 제어 (타원형 유도)
        if (!"auto".equals(direction) && currentNode != null && nextNode != null) {
            double directionBonus = calculateDirectionBonus(
                    currentNode, nextNode, endNode, direction, isFirstSegment
            );
            cost *= (1 - directionBonus);
        }

        return cost;
    }

    // 방향 보너스 계산
    private double calculateDirectionBonus(
            Node current, Node next, Node end,
            String direction, boolean isFirstSegment
    ) {
        double latDiff = next.getLatitude() - current.getLatitude();
        double lngDiff = next.getLongitude() - current.getLongitude();

        double bonus = 0.0;

        switch (direction) {
            case "north": bonus = latDiff > 0 ? 0.2 : 0; break;
            case "south": bonus = latDiff < 0 ? 0.2 : 0; break;
            case "east":  bonus = lngDiff > 0 ? 0.2 : 0; break;
            case "west":  bonus = lngDiff < 0 ? 0.2 : 0; break;
        }

        return bonus;
    }

    // ==================== 왔던 길 회피용 단순 경로 탐색 ====================
    private PathResult findPathWithAvoidance(String start, String end, Set<String> avoidLinks) {
        return findPathWithTargetDistance(start, end, 999.0, avoidLinks, "auto", false);
    }

    // ==================== 유틸리티 메서드 ====================
    private Set<String> extractLinkIds(List<PathNode> path, String startId, String endId) {
        // PathNode에는 linkId가 없으므로 노드 순서로 링크 유추
        Set<String> linkIds = new HashSet<>();

        List<String> nodeIds = new ArrayList<>();
        nodeIds.add(startId);
        // 중간 노드들 추가 필요 (실제로는 PathNode에 nodeId 저장 필요)
        nodeIds.add(endId);

        return linkIds; // 간단히 빈 셋 반환 (실제로는 개선 필요)
    }

    private List<String> reconstructPath(String start, String end, Map<String, String> predecessors) {
        LinkedList<String> path = new LinkedList<>();
        String current = end;

        while (current != null && !current.equals(start)) {
            path.addFirst(current);
            current = predecessors.get(current);
        }

        if (current != null && current.equals(start)) {
            path.addFirst(start);
        }
        return path;
    }

    private List<PathNode> convertToPathNodes(List<String> nodeIds) {
        List<PathNode> result = new ArrayList<>();
        for (String id : nodeIds) {
            Node node = graphService.getNode(id);
            if (node != null) {
                result.add(new PathNode(node.getLatitude(), node.getLongitude()));
            }
        }
        return result;
    }

    private String findNearestNodeId(double lat, double lng) {
        String nearestId = null;
        double minDist = Double.MAX_VALUE;

        for (Node node : graphService.getAllNodes()) {
            if (node.getIsVirtual()) continue;

            double dist = calculateHaversine(lat, lng, node.getLatitude(), node.getLongitude());
            if (dist < minDist) {
                minDist = dist;
                nearestId = node.getNodeId();
            }
        }
        return nearestId;
    }

    private double calculateHaversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double latDist = Math.toRadians(lat2 - lat1);
        double lonDist = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDist / 2) * Math.sin(latDist / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDist / 2) * Math.sin(lonDist / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private String getOppositeDirection(String direction) {
        switch (direction) {
            case "north": return "south";
            case "south": return "north";
            case "east": return "west";
            case "west": return "east";
            default: return "auto";
        }
    }

    // State 내부 클래스
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
}