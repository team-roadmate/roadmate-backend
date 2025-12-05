package com.trm.roadmate_backend.repository;

import com.trm.roadmate_backend.entity.WalkingNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WalkingNodeRepository extends JpaRepository<WalkingNode, Long> {
    Optional<WalkingNode> findByNodeId(String nodeId);
    boolean existsByNodeId(String nodeId);
    long countBySggNm(String sggNm);
}
