package com.trm.roadmate_backend.repository.walk;

import com.trm.roadmate_backend.entity.walk.WalkingRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WalkingRouteRepository extends JpaRepository<WalkingRoute, Long> {

    /**
     * 특정 사용자의 모든 경로를 저장 시각(savedAt)의 최신순으로 조회합니다.
     * (최근 기록 관리와 저장한 코스 조회에 사용)
     */
    List<WalkingRoute> findByUserIdOrderBySavedAtDesc(Long userId);

    // 이외에도 findByUserIdAndTitleIsNotNull 등의 메서드를 추가하여
    // '저장한 코스'만 필터링하는 등의 로직을 구현할 수 있습니다.
}