package com.trm.roadmate_backend.repository;

import com.trm.roadmate_backend.entity.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {
    // linkId가 존재하는지 확인
    boolean existsByLinkId(String linkId);
}