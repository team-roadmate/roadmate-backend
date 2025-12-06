package com.trm.roadmate_backend.service;

import com.trm.roadmate_backend.dto.PathResponse;
import com.trm.roadmate_backend.entity.Link;
import com.trm.roadmate_backend.entity.Node;
import com.trm.roadmate_backend.repository.LinkRepository;
import com.trm.roadmate_backend.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PathFindingService {

    private final NodeRepository nodeRepository;
    private final LinkRepository linkRepository;

    private static final double WALK_SPEED = 1.11; // 4km/h = 1.11 m/s

    public PathResponse findShortestPath(Double startLat, Double startLng,
                                         Double endLat, Double endLng) {
        // 1. 출발지/도착지 근처 노드 찾기
        Node startNode = nodeRepository.findNearestNode(startLat, startLng);
        Node endNode = nodeRepository.findNearestNode(endLat, endLng);

        if (startNode == null || endNode == null) {
            throw new RuntimeException("근처에 노드를 찾을 수 없습니다");
        }

        log.info("Start: {} -> End: {}", startNode.getNodeId(), endNode.getNodeId());

        // 2. A* 알고리즘 실행
        List<String> path = aStarSearch(startNode.getNodeId(), endNode.getNodeId());

        if (path.isEmpty()) {
            throw new RuntimeException("경로를 찾을 수 없습니다");
        }

        // 3. 결과 생성
        return buildPathResponse(path);
    }

    private List<String> aStarSearch(String startId, String endId) {
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(
                Comparator.comparingDouble(n -> n.fScore)
        );

        Map<String, Double> gScore = new HashMap<>();
        Map<String, String> cameFrom = new HashMap<>();
        Set<String> closedSet = new HashSet<>();

        Node endNode = nodeRepository.findByNodeId(endId)
                .orElseThrow(() -> new RuntimeException("End node not found"));

        gScore.put(startId, 0.0);
        openSet.add(new AStarNode(startId, 0.0,
                heuristic(startId, endNode.getLatitude(), endNode.getLongitude())));

        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();

            if (current.nodeId.equals(endId)) {
                return reconstructPath(cameFrom, endId);
            }

            if (closedSet.contains(current.nodeId)) {
                continue;
            }
            closedSet.add(current.nodeId);

            // 이웃 노드 탐색 (보행자 전용)
            List<Link> neighbors = linkRepository.findWalkableLinks(current.nodeId);

            for (Link link : neighbors) {
                String neighborId = link.getEndNodeId();

                if (closedSet.contains(neighborId)) {
                    continue;
                }

                // gScore.get(current.nodeId)가 null일 경우 0.0을 사용 (이미 startId에 0.0을 넣었으므로 null이 될 일은 없지만 안전하게 처리)
                double currentGScore = gScore.getOrDefault(current.nodeId, 0.0);
                double tentativeG = currentGScore + link.getLength();

                if (tentativeG < gScore.getOrDefault(neighborId, Double.MAX_VALUE)) {
                    cameFrom.put(neighborId, current.nodeId);
                    gScore.put(neighborId, tentativeG);

                    double h = heuristic(neighborId, endNode.getLatitude(), endNode.getLongitude());
                    double f = tentativeG + h;

                    // PriorityQueue에 이미 노드가 있을 경우 업데이트하는 로직은 생략되어 있음 (A*의 일반적인 구현에서는 필요)
                    // 여기서는 단순히 새로운 AStarNode를 추가하여, 나중에 더 짧은 경로로 도착한 노드가 먼저 처리되도록 합니다.
                    openSet.add(new AStarNode(neighborId, tentativeG, f));
                }
            }
        }

        return Collections.emptyList();
    }

    private double heuristic(String nodeId, double targetLat, double targetLng) {
        Node node = nodeRepository.findByNodeId(nodeId).orElse(null);
        if (node == null) return 0.0;

        return haversineDistance(node.getLatitude(), node.getLongitude(),
                targetLat, targetLng);
    }

    // Haversine 공식 (두 지점 간 직선거리)
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // 지구 반지름 (미터)

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    private List<String> reconstructPath(Map<String, String> cameFrom, String current) {
        List<String> path = new ArrayList<>();
        path.add(current);

        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(0, current);
        }

        return path;
    }

    private PathResponse buildPathResponse(List<String> nodePath) {
        List<PathResponse.PathNode> pathNodes = new ArrayList<>();
        List<PathResponse.PathLink> pathLinks = new ArrayList<>();
        double totalDistance = 0.0;

        // 노드 정보 추가
        for (String nodeId : nodePath) {
            Node node = nodeRepository.findByNodeId(nodeId).orElse(null);
            if (node != null) {
                pathNodes.add(PathResponse.PathNode.builder()
                        .nodeId(node.getNodeId())
                        .latitude(node.getLatitude())
                        .longitude(node.getLongitude())
                        .nodeType(getNodeTypeName(node.getNodeTypeCd()))
                        .build());
            }
        }

        // 링크 정보 추가 및 거리 계산
        for (int i = 0; i < nodePath.size() - 1; i++) {
            String startNodeId = nodePath.get(i);
            String endNodeId = nodePath.get(i + 1);

            // startNodeId에서 출발하는 보행 가능한 링크들 중, endNodeId로 가는 링크를 찾습니다.
            List<Link> links = linkRepository.findWalkableLinks(startNodeId);
            for (Link link : links) {
                if (link.getEndNodeId().equals(endNodeId)) {
                    pathLinks.add(PathResponse.PathLink.builder()
                            .linkId(link.getLinkId())
                            .length(link.getLength())
                            .roadType(getRoadTypeName(link))
                            .build());
                    totalDistance += link.getLength();
                    break; // 다음 노드로 이동
                }
            }
        }

        int estimatedTime = (int) (totalDistance / WALK_SPEED);

        return PathResponse.builder()
                .totalDistance(totalDistance)
                .estimatedTime(estimatedTime)
                .path(pathNodes)
                .links(pathLinks)
                .build();
    }

    private String getNodeTypeName(String typeCd) {
        if (typeCd == null) return "일반";
        switch (typeCd) {
            case "1": return "지하철출구";
            case "2": return "버스정류장";
            case "3": return "지하도입구";
            default: return "일반";
        }
    }

    private String getRoadTypeName(Link link) {
        if ("Y".equals(link.getPark())) return "공원길";
        if ("Y".equals(link.getSbwyNtw())) return "지하철연결통로";
        if ("Y".equals(link.getBldg())) return "건물내부";
        if ("Y".equals(link.getCrswk())) return "횡단보도";
        return "일반도로";
    }

    // A* 노드 클래스
    private static class AStarNode {
        String nodeId;
        double gScore;
        double fScore;

        AStarNode(String nodeId, double gScore, double fScore) {
            this.nodeId = nodeId;
            this.gScore = gScore;
            this.fScore = fScore;
        }
    }
}