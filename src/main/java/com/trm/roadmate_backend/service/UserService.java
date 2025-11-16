package com.trm.roadmate_backend.service;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.trm.roadmate_backend.entity.User;
import com.trm.roadmate_backend.repository.UserRepository;

// Spring Security 인증 예외 사용
import org.springframework.security.authentication.BadCredentialsException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // 의존성 주입 문제 해결 및 코드 간결화
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ... (생성자 제거, @RequiredArgsConstructor 사용)

    public User registerUser(String name, String email, String rawPassword) {
        // 🌟 1. 이메일 중복 검증 로직 추가 (기본 예외 사용)
        if (userRepository.findByEmail(email).isPresent()) {
            // IllegalStateException은 '메서드 호출이 부적절한 상태'일 때 사용되는 표준 예외입니다.
            throw new IllegalStateException("이미 존재하는 이메일입니다: " + email);
        }

        String encodedPassword = passwordEncoder.encode(rawPassword);
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(encodedPassword);
        return userRepository.save(user);
    }

    public User login(String email, String rawPassword) {
        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(email)
                // 🌟 BadCredentialsException: 인증 정보(자격 증명)가 잘못되었을 때 사용되는 표준 예외
                .orElseThrow(() -> new BadCredentialsException("이메일 또는 비밀번호가 일치하지 않습니다."));

        // 2. 비밀번호 일치 여부 확인
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            // 🌟 비밀번호 불일치 시에도 동일한 BadCredentialsException 발생
            throw new BadCredentialsException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        return user;
    }
}