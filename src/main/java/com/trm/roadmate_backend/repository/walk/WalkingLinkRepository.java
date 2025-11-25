package com.trm.roadmate_backend.repository.walk;

import com.trm.roadmate_backend.entity.walk.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WalkingLinkRepository extends JpaRepository<WalkingLink, Long> {

    // 특정 구역의 모든 링크 조회
    List<WalkingLink> findByDistrict(String district);

    // 시작 노드로 검색
    List<WalkingLink> findByStartNodeId(String startNodeId);

    // 특정 구역의 링크 개수
    long countByDistrict(String district);

    // 특정 구역 삭제
    void deleteByDistrict(String district);
}
