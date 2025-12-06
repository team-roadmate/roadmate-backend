package com.trm.roadmate_backend.repository;

import com.trm.roadmate_backend.entity.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {
    boolean existsByLinkId(String linkId);

    // 보행자 가능한 링크만 (type_cd 첫글자가 1)
    @Query("SELECT l FROM Link l WHERE l.startNodeId = :nodeId AND l.typeCd LIKE '1%'")
    List<Link> findWalkableLinks(String nodeId);
}