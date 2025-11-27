package com.trm.roadmate_backend.repository;


import com.trm.roadmate_backend.entity.ReviewSave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<ReviewSave, Long> {

    // 특정 코스(route)에 대한 리뷰들
    List<ReviewSave> findByRouteId(Long routeId);

    // 특정 유저가 작성한 리뷰들
    List<ReviewSave> findByUserId(Long userId);
}