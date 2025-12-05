package com.trm.roadmate_backend.repository;

import com.trm.roadmate_backend.entity.WalkingLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WalkingLinkRepository extends JpaRepository<WalkingLink, Long> {
    Optional<WalkingLink> findByLinkId(String linkId);
    boolean existsByLinkId(String linkId);
    long countBySggNm(String sggNm);
}
