package com.trm.roadmate_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.trm.roadmate_backend.entity.User;
import java.util.Optional;

// User 엔티티와 기본 키 타입(Integer)을 사용하여 JPA 기본 기능을 상속합니다.
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * 이메일을 기준으로 사용자 엔티티를 조회합니다.
     * Spring Data JPA의 쿼리 메서드 기능을 사용합니다.
     * @param email 조회할 사용자 이메일
     * @return User 객체를 담는 Optional
     */
    Optional<User> findByEmail(String email);
}
