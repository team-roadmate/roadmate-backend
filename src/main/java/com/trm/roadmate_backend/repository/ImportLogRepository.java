package com.trm.roadmate_backend.repository;

import com.trm.roadmate_backend.entity.ImportLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportLogRepository extends JpaRepository<ImportLog, Long> {
}