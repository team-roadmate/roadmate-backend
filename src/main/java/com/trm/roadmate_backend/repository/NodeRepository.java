package com.trm.roadmate_backend.repository;

import com.trm.roadmate_backend.entity.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface NodeRepository extends JpaRepository<Node, Long> {
    Optional<Node> findByNodeId(String nodeId);
    boolean existsByNodeId(String nodeId);

    // 특정 위치에서 가장 가까운 노드 찾기 (Haversine)
    @Query(value =
            "SELECT *, " +
                    "(6371000 * acos(cos(radians(:lat)) * cos(radians(latitude)) " +
                    "* cos(radians(longitude) - radians(:lng)) " +
                    "+ sin(radians(:lat)) * sin(radians(latitude)))) AS distance " +
                    "FROM node " +
                    "WHERE is_virtual = false " +
                    "ORDER BY distance " +
                    "LIMIT 1",
            nativeQuery = true)
    Node findNearestNode(Double lat, Double lng);
}