package com.trm.roadmate_backend.config;

import com.trm.roadmate_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// ✨ import com.trm.roadmate_backend.entity.User; 필요 (생략함)

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. 이메일로 사용자 DB 조회 (없으면 Spring Security가 401 또는 403 처리 유도)
        com.trm.roadmate_backend.entity.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 이메일의 사용자를 찾을 수 없습니다: " + email));

        // 2. CustomUserDetails 객체를 생성하여 userId를 포함해서 반환
        return new CustomUserDetails(user); // ✨ CustomUserDetails 사용
    }
}