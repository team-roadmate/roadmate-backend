package com.trm.roadmate_backend.repository.walk;

import com.trm.roadmate_backend.entity.walk.WalkingLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Repository
public interface WalkingLinkRepository extends JpaRepository<WalkingLink, Long> {

    // Stream 기반 조회 (대량 로딩 방지)
    Stream<WalkingLink> findByDistrict(String district);

    long countByDistrict(String district);

    @Modifying
    @Transactional
    @Query("DELETE FROM WalkingLink l WHERE l.district = :district")
    void deleteByDistrictFast(@Param("district") String district);

    List<WalkingLink> findByLinkIdIn(Set<String> linkIds);

    // 그래프 구축용: 특정 startNode 모든 link 조회
    List<WalkingLink> findByStartNodeId(String startNodeId);

    // linkId unique 기반 검색 추가
    List<WalkingLink> findByLinkId(String linkId);
}
