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

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    // ⭐️ [추가된 부분 1] Swagger UI 및 API 문서 관련 경로 정의
    private static final String[] SWAGGER_WHITELIST = {
            "/v3/api-docs/**",      // OpenAPI 3 (JSON/YAML)
            "/swagger-ui/**",       // Swagger UI HTML 및 리소스 (버전 3 이상)
            "/swagger-resources/**",
            "/swagger-ui.html",
            "/webjars/**"
    };

    /**
     * 비밀번호 암호화를 위한 BCryptPasswordEncoder 빈 등록
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 보안 필터 체인을 구성합니다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 보호 기능 비활성화 (REST API는 세션을 사용하지 않음)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. HTTP Basic, Form Login 등 기본 인증 방식 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                // 3. 세션 STATELESS 설정 (JWT 기반이므로 서버에 상태를 저장하지 않음)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 4. 요청별 접근 권한 설정 (인가)
                .authorizeHttpRequests(auth -> auth
                        // ⭐️ [수정/추가된 부분 2] Swagger 경로를 인증 없이 접근 허용
                        .requestMatchers(SWAGGER_WHITELIST).permitAll()

                        // 로그인, 회원가입, 에러 경로는 인증 없이 접근 허용 (기존 로직)
                        .requestMatchers("/api/auth/**", "/error").permitAll()

                        // 그 외 모든 요청은 인증된 사용자만 접근 허용
                        .anyRequest().authenticated()
                );

        // 5. JWT 필터를 표준 인증 필터 이전에 추가하여 토큰 기반 인증 수행
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}