package com.trm.roadmate_backend.service.walk;

import com.trm.roadmate_backend.entity.walk.*;
import com.trm.roadmate_backend.repository.walk.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphService {

    private final WalkingNodeRepository nodeRepository;
    private final WalkingLinkRepository linkRepository;

    // 메모리 그래프 구조
    private final Map<String, Node> nodeMap = new ConcurrentHashMap<>();
    private final Map<String, List<Edge>> adjacencyList = new ConcurrentHashMap<>();

    /**
     * 서버 시작 시 그래프 구축
     */
    @PostConstruct
    public void buildGraph() {
        log.info("=== 메모리 그래프 구축 시작 ===");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 모든 노드 로드
            List<WalkingNode> nodes = nodeRepository.findAll();
            log.info("노드 로드 완료: {} 개", nodes.size());

            nodes.forEach(n -> {
                String nodeId = n.getNodeId().trim();
                Node node = new Node(
                        nodeId,
                        n.getLatitude(),
                        n.getLongitude(),
                        n.getDistrict(),
                        n.getIsPark()
                );
                nodeMap.put(nodeId, node);
                adjacencyList.put(nodeId, new ArrayList<>());
            });

            // 2. 모든 링크 로드
            List<WalkingLink> links = linkRepository.findAll();
            log.info("링크 로드 완료: {} 개", links.size());

            links.forEach(l -> {
                String startId = l.getStartNodeId().trim();
                String endId = l.getEndNodeId().trim();

                // 💡 [수정] 노드 맵에 키가 존재하는지 확인 및 누락 로그 출력
                if (!nodeMap.containsKey(startId) || !nodeMap.containsKey(endId)) {
                    log.warn("누락 링크 발생: Link ID {} ({}) -> ({}) - 노드 맵에 없음 (ID 불일치 가능성)",
                            l.getLinkId(), startId, endId);
                    return;
                }

                Edge edge = new Edge(
                        endId,
                        l.getDistance(),
                        l.getIsPark(),
                        l.getIsOverpass(),
                        l.getIsTunnel(),
                        l.getIsBuilding()
                );

                // 양방향 그래프 (startId -> endId)
                adjacencyList.computeIfAbsent(startId, k -> new ArrayList<>()).add(edge);

                Edge reverseEdge = new Edge(
                        startId,
                        l.getDistance(),
                        l.getIsPark(),
                        l.getIsOverpass(),
                        l.getIsTunnel(),
                        l.getIsBuilding()
                );
                // 양방향 그래프 (endId -> startId)
                adjacencyList.computeIfAbsent(endId, k -> new ArrayList<>()).add(reverseEdge);
            });

            long edgeCount = adjacencyList.values().stream().mapToInt(List::size).sum();

            long endTime = System.currentTimeMillis();
            log.info("=== 그래프 구축 완료: 노드 {}, 간선 {}, 소요시간 {}ms ===",
                    nodeMap.size(), edgeCount, endTime - startTime);

        } catch (Exception e) {
            log.error("그래프 구축 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 그래프 재구축 (데이터 수집 후 호출)
     */
    public void rebuildGraph() {
        nodeMap.clear();
        adjacencyList.clear();
        buildGraph();
    }

    /**
     * 노드 조회
     */
    public Node getNode(String nodeId) {
        return nodeMap.get(nodeId);
    }

    /**
     * 모든 노드 조회
     */
    public Collection<Node> getAllNodes() {
        return nodeMap.values();
    }

    /**
     * 인접 간선 조회
     */
    public List<Edge> getEdges(String nodeId) {
        return adjacencyList.getOrDefault(nodeId, Collections.emptyList());
    }

    /**
     * 특정 구역의 노드만 조회
     */
    public List<Node> getNodesByDistrict(String district) {
        return nodeMap.values().stream()
                .filter(n -> district.equals(n.getDistrict()))
                .toList();
    }

    /**
     * 그래프가 비어있는지 확인
     */
    public boolean isEmpty() {
        return nodeMap.isEmpty();
    }

    /**
     * 그래프 통계 정보
     */
    public GraphStats getStats() {
        return GraphStats.builder()
                .nodeCount(nodeMap.size())
                .edgeCount(adjacencyList.values().stream().mapToInt(List::size).sum())
                .districts(nodeMap.values().stream()
                        .map(Node::getDistrict)
                        .distinct()
                        .toList())
                .build();
    }

    // ===== 내부 클래스 =====

    @Data
    @AllArgsConstructor
    public static class Node {
        private String id;
        private Double lat;
        private Double lng;
        private String district;
        private Boolean isPark;
    }

    @Data
    @AllArgsConstructor
    public static class Edge {
        private String targetNodeId;
        private Double distance;
        private Boolean isPark;
        private Boolean isOverpass;
        private Boolean isTunnel;
        private Boolean isBuilding;
    }

    @Data
    @Builder
    public static class GraphStats {
        private Integer nodeCount;
        private Integer edgeCount;
        private List<String> districts;
    }
}