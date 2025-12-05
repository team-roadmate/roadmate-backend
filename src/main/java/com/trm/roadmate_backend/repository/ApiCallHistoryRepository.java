package com.trm.roadmate_backend.repository;

import com.trm.roadmate_backend.entity.ApiCallHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiCallHistoryRepository extends JpaRepository<ApiCallHistory, Long> {
}
