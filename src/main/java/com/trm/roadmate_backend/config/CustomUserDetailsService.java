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
     * Spring Security의 핵심 메서드. 이메일을 기반으로 사용자 정보를 로드하고 권한을 부여합니다.
     * @param email 사용자 ID로 사용되는 이메일
     * @return 인증을 위한 UserDetails 객체
     * @throws UsernameNotFoundException 해당 이메일의 사용자가 없을 경우
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. 이메일로 사용자 DB 조회 (없으면 예외 발생)
        com.trm.roadmate_backend.entity.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 이메일의 사용자를 찾을 수 없습니다: " + email));

        // 2. 모든 인증된 사용자에게 'ROLE_USER' 권한 부여
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        // 3. UserDetails 객체를 생성하여 Spring Security에 전달
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }
}
