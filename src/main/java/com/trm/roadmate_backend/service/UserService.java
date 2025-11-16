package com.trm.roadmate_backend.service;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.trm.roadmate_backend.entity.User;
import com.trm.roadmate_backend.repository.UserRepository;

import org.springframework.security.authentication.BadCredentialsException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 새로운 사용자를 등록하고 비밀번호를 암호화하여 저장합니다.
     * @param name 사용자 이름
     * @param email 사용자 이메일
     * @param rawPassword 암호화되지 않은 비밀번호
     * @return 저장된 User 엔티티
     */
    public User registerUser(String name, String email, String rawPassword) {
        // 이메일 중복 검증
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("이미 존재하는 이메일입니다: " + email);
        }

        String encodedPassword = passwordEncoder.encode(rawPassword);
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(encodedPassword);
        return userRepository.save(user);
    }

    /**
     * 사용자 이메일과 비밀번호를 검증하여 로그인 처리를 수행합니다.
     * @param email 사용자 이메일
     * @param rawPassword 암호화되지 않은 비밀번호
     * @return 로그인 성공 시 User 엔티티
     * @throws BadCredentialsException 이메일 또는 비밀번호가 일치하지 않을 경우
     */
    public User login(String email, String rawPassword) {
        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("이메일 또는 비밀번호가 일치하지 않습니다."));

        // 2. 비밀번호 일치 여부 확인
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BadCredentialsException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        return user;
    }
}
