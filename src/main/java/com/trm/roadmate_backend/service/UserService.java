package com.trm.roadmate_backend.service;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.trm.roadmate_backend.entity.User;
import com.trm.roadmate_backend.repository.UserRepository;

import org.springframework.security.authentication.BadCredentialsException;
import lombok.RequiredArgsConstructor;

import java.util.NoSuchElementException;

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
        // user.setRefreshToken(null) 은 생략 (기본값 null)

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

    /**
     * [구현 완료] 로그인 성공 또는 리프레시 성공 후 발급된 리프레시 토큰을 사용자 엔티티에 저장(갱신)합니다.
     * @param email 사용자 이메일
     * @param refreshToken 새로 발급된 리프레시 토큰
     */
    public void saveRefreshToken(String email, String refreshToken) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        // 새로 발급된 토큰으로 갱신
        user.setRefreshToken(refreshToken);

        userRepository.save(user);
    }

    /**
     * [구현 완료] 리프레시 토큰의 유효성을 검증하고, 서버에 저장된 토큰과 일치하는지 확인합니다.
     * @param email 토큰에서 추출한 사용자 이메일
     * @param incomingRefreshToken 클라이언트가 보낸 리프레시 토큰
     * @return 유효하면 true, 아니면 false
     */
    public boolean validateStoredRefreshToken(String email, String incomingRefreshToken) {
        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            // 사용자 없음
            return false;
        }

        // 핵심 로직: 클라이언트가 보낸 토큰과 DB에 저장된 토큰이 일치하는지 확인
        // 일치하지 않으면 (예: 이미 사용된 토큰이거나 토큰이 탈취되어 갱신된 경우), 무효 처리
        return user.getRefreshToken() != null && user.getRefreshToken().equals(incomingRefreshToken);
    }
}
