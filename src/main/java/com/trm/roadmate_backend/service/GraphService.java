package com.trm.roadmate_backend.service;

import com.trm.roadmate_backend.entity.Node;
import com.trm.roadmate_backend.entity.Link;
import com.trm.roadmate_backend.repository.LinkRepository;
import com.trm.roadmate_backend.repository.NodeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphService {

    // --- 인메모리 그래프 ---
    private final Map<String, List<Edge>> adjacencyMap = new HashMap<>();
    private final Map<String, Node> nodeMap = new HashMap<>();

    private final NodeRepository nodeRepository;
    private final LinkRepository linkRepository;

    // --- Public API ---
    public Collection<Node> getAllNodes() {
        return nodeMap.values();
    }

    public List<Edge> getEdges(String nodeId) {
        return adjacencyMap.getOrDefault(nodeId, Collections.emptyList());
    }

    public Node getNode(String nodeId) {
        return nodeMap.get(nodeId);
    }

    // Edge 클래스
    public static class Edge {
        public final String destinationId;
        public final double weight;
        public final String linkId;  // ⭐ 추가

        public Edge(String destinationId, double weight, String linkId) {
            this.destinationId = destinationId;
            this.weight = weight;
            this.linkId = linkId;
        }
    }

    // --- 초기화 & 재로딩 ---
    @PostConstruct
    @Transactional(readOnly = true)
    public void init() {
        buildGraph();
    }

    @Transactional(readOnly = true)
    public synchronized void reloadGraph() {
        log.info("Graph reload requested");
        buildGraph();
    }

    // --- 그래프 빌드 ---
    @Transactional(readOnly = true)
    protected void buildGraph() {
        long startTime = System.currentTimeMillis();
        adjacencyMap.clear();
        nodeMap.clear();

        log.info("Loading nodes from DB...");

        // 1) 모든 노드 로딩 (페이징)
        int page = 0;
        int size = 5000;
        Page<Node> nodePage;
        AtomicInteger totalNodes = new AtomicInteger(0);

        do {
            nodePage = nodeRepository.findAll(PageRequest.of(page++, size));
            nodePage.forEach(node -> nodeMap.put(node.getNodeId(), node));
            totalNodes.addAndGet(nodePage.getNumberOfElements());
        } while (nodePage.hasNext());

        log.info("Total nodes loaded: {}", totalNodes.get());

        log.info("Loading links from DB...");

        // 2) 모든 링크 로딩 (페이징)
        page = 0;
        size = 5000;
        Page<Link> linkPage;
        AtomicInteger totalEdges = new AtomicInteger(0);

        do {
            linkPage = linkRepository.findAll(PageRequest.of(page++, size));
            linkPage.forEach(link -> {
                double length = link.getLength() == null ? 0.0 : link.getLength();

                if (!nodeMap.containsKey(link.getStartNodeId())) {
                    log.warn("Missing start node for edge {} -> {}", link.getStartNodeId(), link.getEndNodeId());
                } else if (!nodeMap.containsKey(link.getEndNodeId())) {
                    log.warn("Missing end node for edge {} -> {}", link.getStartNodeId(), link.getEndNodeId());
                } else {
                    addEdge(link.getStartNodeId(), link.getEndNodeId(), length, link.getLinkId());
                    addEdge(link.getEndNodeId(), link.getStartNodeId(), length, link.getLinkId());
                    totalEdges.addAndGet(2);
                }
            });
        } while (linkPage.hasNext());

        long endTime = System.currentTimeMillis();
        log.info("⭐ Graph Loaded Success ⭐");
        log.info("Total Nodes = {}", nodeMap.size());
        log.info("Total Edges = {}", totalEdges.get());
        log.info("Graph Loading Time: {} ms", (endTime - startTime));
    }

    private void addEdge(String source, String destination, double weight, String linkId) {
        adjacencyMap.computeIfAbsent(source, k -> new ArrayList<>())
                .add(new Edge(destination, weight, linkId));
    }
}
