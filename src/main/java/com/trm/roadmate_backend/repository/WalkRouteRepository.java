package com.trm.roadmate_backend.repository;

import com.trm.roadmate_backend.entity.WalkRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WalkRouteRepository extends JpaRepository<WalkRoute, Long> {

    // 소유권 검증 및 데이터 로드에 사용
    Optional<WalkRoute> findByRouteIdAndUserId(Long routeId, Integer userId);

    // [GET /routes/history] 전체 기록 목록 조회
    List<WalkRoute> findByUserIdAndIsDeletedOrderByStartTimeDesc(Integer userId, boolean isDeleted);

    // [GET /routes/courses] 저장된 코스 목록 조회
    List<WalkRoute> findByUserIdAndIsCourseAndIsDeletedOrderByStartTimeDesc(Integer userId, boolean isCourse, boolean isDeleted);

    // 소프트 삭제를 위한 존재 여부 검증 (선택적)
    boolean existsByRouteIdAndUserId(Long routeId, Integer userId);
}