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

    private final Map<String, Node> nodeMap = new ConcurrentHashMap<>();
    private final Map<String, List<Edge>> adjacencyList = new ConcurrentHashMap<>();

    @PostConstruct
    public void buildGraph() {
        log.info("=== 메모리 그래프 구축 시작 ===");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 모든 노드 로드
            List<WalkingNode> nodes = nodeRepository.findAll();
            log.info("노드 로드 완료: {} 개", nodes.size());

            // 노드 ID 중복 체크
            Set<String> nodeIdCheck = new HashSet<>();
            int duplicateNodes = 0;

            for (WalkingNode n : nodes) {
                String nodeId = normalizeId(n.getNodeId());

                if (nodeIdCheck.contains(nodeId)) {
                    duplicateNodes++;
                    log.warn("⚠️ 중복 노드 ID 발견: {}", nodeId);
                    continue;
                }

                nodeIdCheck.add(nodeId);

                Node node = new Node(
                        nodeId,
                        n.getLatitude(),
                        n.getLongitude(),
                        n.getDistrict(),
                        n.getIsPark()
                );
                nodeMap.put(nodeId, node);
                adjacencyList.put(nodeId, new ArrayList<>());
            }

            if (duplicateNodes > 0) {
                log.warn("⚠️ 중복 노드 총 {}개 스킵됨", duplicateNodes);
            }

            // 2. 모든 링크 로드
            List<WalkingLink> links = linkRepository.findAll();
            log.info("링크 로드 완료: {} 개", links.size());

            int skippedLinks = 0;
            int addedEdges = 0;
            Map<String, Integer> skipReasons = new HashMap<>();

            for (WalkingLink l : links) {
                String startId = normalizeId(l.getStartNodeId());
                String endId = normalizeId(l.getEndNodeId());

                // 🔍 디버깅: 링크 ID 샘플 출력 (처음 3개만)
                if (addedEdges + skippedLinks < 3) {
                    log.info("🔍 링크 샘플: {} -> {} (원본: {} -> {})",
                            startId, endId, l.getStartNodeId(), l.getEndNodeId());
                }

                // 검증 1: 시작 노드 존재 여부
                if (!nodeMap.containsKey(startId)) {
                    skippedLinks++;
                    skipReasons.merge("시작노드없음", 1, Integer::sum);
                    if (skippedLinks <= 5) { // 처음 5개만 로그
                        log.warn("❌ 시작 노드 없음: Link {} -> Start '{}' (원본: '{}')",
                                l.getLinkId(), startId, l.getStartNodeId());
                    }
                    continue;
                }

                // 검증 2: 종료 노드 존재 여부
                if (!nodeMap.containsKey(endId)) {
                    skippedLinks++;
                    skipReasons.merge("종료노드없음", 1, Integer::sum);
                    if (skippedLinks <= 5) {
                        log.warn("❌ 종료 노드 없음: Link {} -> End '{}' (원본: '{}')",
                                l.getLinkId(), endId, l.getEndNodeId());
                    }
                    continue;
                }

                // 검증 3: 거리 유효성
                if (l.getDistance() == null || l.getDistance() <= 0) {
                    skippedLinks++;
                    skipReasons.merge("거리오류", 1, Integer::sum);
                    continue;
                }

                // 검증 4: 자기 자신으로의 연결 방지
                if (startId.equals(endId)) {
                    skippedLinks++;
                    skipReasons.merge("자기참조", 1, Integer::sum);
                    continue;
                }

                // 간선 추가 (양방향)
                Edge edge = new Edge(
                        endId,
                        l.getDistance(),
                        l.getIsPark(),
                        l.getIsOverpass(),
                        l.getIsTunnel(),
                        l.getIsBuilding()
                );
                adjacencyList.get(startId).add(edge);
                addedEdges++;

                Edge reverseEdge = new Edge(
                        startId,
                        l.getDistance(),
                        l.getIsPark(),
                        l.getIsOverpass(),
                        l.getIsTunnel(),
                        l.getIsBuilding()
                );
                adjacencyList.get(endId).add(reverseEdge);
                addedEdges++;
            }

            // 3. 통계 출력
            long edgeCount = adjacencyList.values().stream().mapToInt(List::size).sum();
            long isolatedNodes = adjacencyList.values().stream()
                    .filter(List::isEmpty)
                    .count();

            long endTime = System.currentTimeMillis();

            log.info("=== 그래프 구축 완료 ===");
            log.info("노드: {} 개", nodeMap.size());
            log.info("간선: {} 개 (추가된 간선: {})", edgeCount, addedEdges);
            log.info("스킵된 링크: {} 개", skippedLinks);
            log.info("고립된 노드: {} 개 ({:.1f}%)",
                    isolatedNodes, (isolatedNodes * 100.0 / nodeMap.size()));
            log.info("소요시간: {}ms", endTime - startTime);

            // 스킵 이유 상세
            if (!skipReasons.isEmpty()) {
                log.info("📊 링크 스킵 상세:");
                skipReasons.forEach((reason, count) ->
                        log.info("  - {}: {} 건", reason, count));
            }

            // 4. 연결성 검증 (샘플링)
            validateGraphConnectivity();

            // 5. 노드 연결 분포 분석
            analyzeNodeDegreeDistribution();

        } catch (Exception e) {
            log.error("그래프 구축 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 그래프 연결성 검증 (BFS로 가장 큰 연결 컴포넌트 찾기)
     */
    private void validateGraphConnectivity() {
        if (nodeMap.isEmpty()) return;

        Set<String> visited = new HashSet<>();
        int largestComponent = 0;
        int componentCount = 0;

        for (String nodeId : nodeMap.keySet()) {
            if (!visited.contains(nodeId)) {
                int componentSize = bfsComponentSize(nodeId, visited);
                largestComponent = Math.max(largestComponent, componentSize);
                componentCount++;
            }
        }

        log.info("🔗 연결성 분석:");
        log.info("  - 연결 컴포넌트 개수: {}", componentCount);
        log.info("  - 최대 컴포넌트 크기: {} 노드 ({:.1f}%)",
                largestComponent, (largestComponent * 100.0 / nodeMap.size()));

        if (componentCount > 1) {
            log.warn("⚠️ 그래프가 {} 개의 분리된 영역으로 나뉘어져 있습니다!", componentCount);
        }
    }

    /**
     * BFS로 연결된 컴포넌트 크기 계산
     */
    private int bfsComponentSize(String startNode, Set<String> visited) {
        Queue<String> queue = new LinkedList<>();
        queue.offer(startNode);
        visited.add(startNode);
        int size = 0;

        while (!queue.isEmpty()) {
            String current = queue.poll();
            size++;

            List<Edge> edges = adjacencyList.getOrDefault(current, Collections.emptyList());
            for (Edge edge : edges) {
                String next = edge.getTargetNodeId();
                if (!visited.contains(next)) {
                    visited.add(next);
                    queue.offer(next);
                }
            }
        }

        return size;
    }

    /**
     * 노드 연결 개수 분포 분석
     */
    private void analyzeNodeDegreeDistribution() {
        Map<Integer, Integer> degreeDistribution = new HashMap<>();

        for (List<Edge> edges : adjacencyList.values()) {
            int degree = edges.size();
            degreeDistribution.merge(degree, 1, Integer::sum);
        }

        log.info("📊 노드 연결 개수 분포:");
        degreeDistribution.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(10) // 상위 10개만
                .forEach(entry ->
                        log.info("  - 연결 {}개: {} 노드", entry.getKey(), entry.getValue()));

        // 평균 연결 개수
        double avgDegree = adjacencyList.values().stream()
                .mapToInt(List::size)
                .average()
                .orElse(0.0);
        log.info("  - 평균 연결 개수: {:.2f}", avgDegree);
    }

    /**
     * ID 정규화
     */
    private String normalizeId(String raw) {
        if (raw == null) return null;
        String s = raw.trim();

        if (s.matches("^\\d+\\.0+$")) {
            s = s.substring(0, s.indexOf('.'));
        }

        if (s.contains(".")) {
            try {
                double d = Double.parseDouble(s);
                long asLong = (long) d;
                if (Double.compare(d, (double) asLong) == 0) {
                    s = String.valueOf(asLong);
                }
            } catch (NumberFormatException ignored) {}
        }
        return s;
    }

    public void rebuildGraph() {
        nodeMap.clear();
        adjacencyList.clear();
        buildGraph();
    }

    public Node getNode(String nodeId) {
        return nodeMap.get(nodeId);
    }

    public Collection<Node> getAllNodes() {
        return nodeMap.values();
    }

    public List<Edge> getEdges(String nodeId) {
        return adjacencyList.getOrDefault(nodeId, Collections.emptyList());
    }

    public List<Node> getNodesByDistrict(String district) {
        return nodeMap.values().stream()
                .filter(n -> district.equals(n.getDistrict()))
                .toList();
    }

    public boolean isEmpty() {
        return nodeMap.isEmpty();
    }

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