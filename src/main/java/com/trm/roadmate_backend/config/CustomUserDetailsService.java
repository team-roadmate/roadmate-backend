package com.trm.roadmate_backend.config;

import com.trm.roadmate_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * 핵심 메서드: 이메일을 기반으로 사용자 정보를 로드하여 UserDetails 객체로 반환합니다.
     * @param email DB에서 조회할 사용자의 이메일 (인증 주체)
     * @throws UsernameNotFoundException 해당 이메일의 사용자가 없을 경우 발생
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. UserRepository를 통해 DB에서 User 엔티티 조회
        com.trm.roadmate_backend.entity.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 이메일의 사용자를 찾을 수 없습니다: " + email));

        // 2. 권한 생성 및 배정 (핵심 로직)
        // 비즈니스 로직상 관리자가 없으므로, 모든 인증된 사용자에게 'ROLE_USER' 권한을 부여합니다.
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        // 3. Spring Security에서 사용하는 UserDetails 객체를 반환
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }
}