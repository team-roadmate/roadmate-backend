package com.trm.roadmate_backend.repository.walk;

import com.trm.roadmate_backend.entity.walk.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DistrictRepository extends JpaRepository<CollectedDistrict, Long> {

    // 구역명으로 조회
    Optional<CollectedDistrict> findByDistrictName(String districtName);

    // 구역이 이미 수집되었는지 확인
    boolean existsByDistrictName(String districtName);
}