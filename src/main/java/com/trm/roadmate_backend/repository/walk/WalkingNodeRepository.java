package com.trm.roadmate_backend.repository.walk;

import com.trm.roadmate_backend.entity.walk.WalkingNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Repository
public interface WalkingNodeRepository extends JpaRepository<WalkingNode, Long> {

    // PK가 아닌 nodeId로 unique 검색
    Optional<WalkingNode> findByNodeId(String nodeId);

    // 메모리 폭주 방지를 위해 Stream 사용
    Stream<WalkingNode> findByDistrict(String district);

    long countByDistrict(String district);

    // 빠른 대량 삭제 (엔티티 조회 없이 바로 DELETE)
    @Modifying
    @Transactional
    @Query("DELETE FROM WalkingNode n WHERE n.district = :district")
    void deleteByDistrictFast(@Param("district") String district);

    // 특정 nodeId 목록이 DB에 이미 존재하는지 확인 (중복 방지용)
    List<WalkingNode> findByNodeIdIn(Set<String> nodeIds);

    // 사각형 범위 필터링
    @Query("""
        SELECT n FROM WalkingNode n
        WHERE n.latitude BETWEEN :minLat AND :maxLat
        AND n.longitude BETWEEN :minLng AND :maxLng
    """)
    List<WalkingNode> searchInBoundingBox(
            double minLat, double maxLat,
            double minLng, double maxLng
    );

    // 거리 기준 정렬 포함한 "가까운 노드 찾기"
    @Query("""
        SELECT n FROM WalkingNode n
        WHERE n.latitude BETWEEN :minLat AND :maxLat
        AND n.longitude BETWEEN :minLng AND :maxLng
        ORDER BY (
            6371 * acos(
                cos(radians(:lat)) * cos(radians(n.latitude)) *
                cos(radians(n.longitude) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(n.latitude))
            )
        )
    """)
    List<WalkingNode> findNearestNodes(
            double lat, double lng,
            double minLat, double maxLat,
            double minLng, double maxLng
    );
}
