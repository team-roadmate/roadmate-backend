package com.trm.roadmate_backend.config;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    // ⭐️ [추가된 부분 1] 인증 필터를 건너뛸 Swagger 관련 경로 목록 정의
    private static final List<String> SWAGGER_WHITELIST = Arrays.asList(
            "/v3/api-docs",         // API 문서 JSON/YAML
            "/swagger-ui",          // Swagger UI 기본 경로
            "/swagger-resources",   // Swagger 리소스
            "/webjars"              // UI 관련 리소스
            // 참고: "/swagger-ui.html"는 "/swagger-ui"로 커버되는 경우가 많습니다.
            // "/api/auth/"는 기존 로직에서 처리하므로 여기서 제외
    );


    // 생성자 주입
    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // ⭐️ [추가된 부분 2] Swagger 관련 경로 검사 로직
        // path.startsWith()를 사용하여 목록의 각 요소로 시작하는지 확인합니다.
        if (isSwaggerPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. 인증 불필요 경로 (로그인/회원가입/토큰 재발급)는 필터 검증을 건너뛰고 바로 진행 (기존 로직)
        if (path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        String email = null;
        String token = null;

        // 2. Authorization 헤더 확인 및 토큰 추출
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // "Bearer " 제거

            try {
                // 토큰 유효성 검사 및 이메일 추출 시도
                if (jwtUtil.validateToken(token)) {
                    email = jwtUtil.extractEmail(token);
                }
            } catch (JwtException | IllegalArgumentException e) {
                // 토큰 만료 또는 변조된 경우
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
                response.getWriter().write("Access token is invalid or expired.");
                return; // 필터 체인 진행 중단
            }

        } else {
            // 토큰이 없으면 다음 필터로 진행 (인증이 필요 없는 리소스에 접근할 경우를 대비)
            // 💡 참고: 만약 모든 나머지 경로가 인증을 요구한다면, 여기서 401을 반환해야 할 수도 있습니다.
            // 현재는 Spring Security의 다음 필터/핸들러가 권한을 처리하도록 맡기는 구조입니다.
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 이메일이 유효하고, Security Context에 인증 정보가 없는 경우에만 인증 처리 (기존과 동일)
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // UserDetailsService를 통해 UserDetails 객체 로드
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // 인증 토큰 생성 (UserDetails와 권한 정보 포함)
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            // Security Context에 인증 객체를 등록
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // 다음 필터 체인 진행
        filterChain.doFilter(request, response);
    }

    // ⭐️ [추가된 부분 3] Swagger 경로를 확인하는 헬퍼 메서드
    private boolean isSwaggerPath(String path) {
        for (String swaggerPath : SWAGGER_WHITELIST) {
            if (path.startsWith(swaggerPath)) {
                return true;
            }
        }
        return false;
    }
}