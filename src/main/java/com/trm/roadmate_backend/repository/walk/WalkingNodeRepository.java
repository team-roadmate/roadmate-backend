package com.trm.roadmate_backend.repository.walk;

import com.trm.roadmate_backend.entity.walk.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalkingNodeRepository extends JpaRepository<WalkingNode, Long> {

    // 특정 구역의 모든 노드 조회
    List<WalkingNode> findByDistrict(String district);

    // 노드 ID로 조회
    Optional<WalkingNode> findByNodeId(String nodeId);

    // 특정 구역의 노드 개수
    long countByDistrict(String district);

    // 좌표 범위 내 노드 검색 (가장 가까운 노드 찾기용)
    @Query("SELECT n FROM WalkingNode n WHERE " +
            "n.latitude BETWEEN :minLat AND :maxLat AND " +
            "n.longitude BETWEEN :minLng AND :maxLng")
    List<WalkingNode> findByLocationRange(
            Double minLat, Double maxLat,
            Double minLng, Double maxLng
    );

    // 특정 구역 삭제
    void deleteByDistrict(String district);
}
