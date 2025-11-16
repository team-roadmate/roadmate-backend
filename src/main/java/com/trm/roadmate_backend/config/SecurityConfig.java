package com.trm.roadmate_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor; // 🌟 Lombok 추가 (생성자 주입 간결화)

@Configuration
@RequiredArgsConstructor // 🌟 생성자 주입을 자동으로 처리합니다.
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter; // 주입 유지

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 비활성화 (REST API 표준)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. HTTP Basic 인증 및 Form Login 비활성화
                .httpBasic(AbstractHttpConfigurer::disable) // 🌟 HTTP Basic 비활성화 명시
                .formLogin(AbstractHttpConfigurer::disable) // 🌟 Form Login 비활성화 명시

                // 3. 세션 STATELESS 설정
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 4. 요청별 접근 권한 설정 (인가)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/error").permitAll() // 로그인/회원가입/에러는 모두 허용
                        .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
                );

        // 5. JWT 필터를 표준 인증 필터 이전에 추가하여 토큰 검증 수행
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}