package com.trm.roadmate_backend.repository;

import com.trm.roadmate_backend.entity.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface NodeRepository extends JpaRepository<Node, Long> {
    // nodeId로 노드를 찾아 Optional로 반환 (사용되지 않았으나 유용)
    Optional<Node> findByNodeId(String nodeId);

    // nodeId가 존재하는지 확인
    boolean existsByNodeId(String nodeId);
}