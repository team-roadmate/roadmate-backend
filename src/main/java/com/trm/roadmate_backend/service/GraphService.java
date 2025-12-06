package com.trm.roadmate_backend.service;

import com.trm.roadmate_backend.entity.Node;
import com.trm.roadmate_backend.repository.LinkRepository;
import com.trm.roadmate_backend.repository.NodeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphService {
    /**
     * 모든 노드 객체의 컬렉션을 반환하여 좌표 스냅핑에 사용됩니다.
     */
    public Collection<Node> getAllNodes() {
        return nodeMap.values();
    }

    private final NodeRepository nodeRepository;
    private final LinkRepository linkRepository;

    // 인접 리스트: [출발 노드 ID] -> [연결된 간선(Edge) 리스트]
    private final Map<String, List<Edge>> adjacencyMap = new HashMap<>();
    // 노드 좌표 맵: [노드 ID] -> [노드 객체]
    private final Map<String, Node> nodeMap = new HashMap<>();

    /**
     * 경로 탐색에 사용되는 간선(Edge) 클래스.
     * 도착 노드와 가중치(거리)를 저장합니다.
     */
    public static class Edge {
        public final String destinationId;
        public final double weight; // Link.length

        public Edge(String destinationId, double weight) {
            this.destinationId = destinationId;
            this.weight = weight;
        }
    }

    /**
     * 애플리케이션 시작 시 DB의 모든 노드와 링크 정보를 로드하여
     * 인메모리 그래프(adjacencyMap, nodeMap)를 구성합니다.
     */
    @PostConstruct
    @Transactional(readOnly = true)
    public void loadGraphData() {
        long startTime = System.currentTimeMillis();

        // 기존 데이터 초기화
        adjacencyMap.clear();
        nodeMap.clear();

        // Step 1: 모든 노드 정보를 로드하여 nodeMap 구성
        nodeRepository.findAll().forEach(node -> {
            nodeMap.put(node.getNodeId(), node);
        });

        // Step 2: 모든 링크 정보를 로드하여 adjacencyMap 구성 (양방향 연결)
        linkRepository.findAll().forEach(link -> {
            // 정방향 (Start -> End)
            // 주의: DB에 저장된 length를 가중치로 사용
            addEdge(link.getStartNodeId(), link.getEndNodeId(), link.getLength());

            // 역방향 (End -> Start) - 도보 네트워크의 대다수는 양방향 통행을 가정
            addEdge(link.getEndNodeId(), link.getStartNodeId(), link.getLength());
        });

        long endTime = System.currentTimeMillis();

        log.info("⭐ Graph Loaded Success ⭐");
        log.info("Total Nodes = {}", nodeMap.size());
        log.info("Total Edges = {}", adjacencyMap.values().stream().mapToLong(List::size).sum());
        log.info("Loading Time: {} ms", (endTime - startTime));
    }

    /**
     * 특정 노드 ID에서 연결된 모든 간선(Edge) 리스트를 반환합니다.
     * @param nodeId 출발 노드 ID
     * @return 연결된 Edge 리스트
     */
    public List<Edge> getEdges(String nodeId) {
        return adjacencyMap.getOrDefault(nodeId, Collections.emptyList());
    }

    /**
     * 특정 노드 ID의 좌표 정보를 포함한 Node 객체를 반환합니다.
     * @param nodeId 노드 ID
     * @return Node 객체 (좌표 정보 포함)
     */
    public Node getNode(String nodeId) {
        return nodeMap.get(nodeId);
    }

    // --- Private Helper Methods ---

    /**
     * 인접 리스트에 새로운 간선을 추가합니다.
     */
    private void addEdge(String source, String destination, double weight) {
        // computeIfAbsent: source 키가 없으면 새 ArrayList를 생성하고 값을 넣은 후 리턴
        adjacencyMap.computeIfAbsent(source, k -> new ArrayList<>())
                .add(new Edge(destination, weight));
    }
}