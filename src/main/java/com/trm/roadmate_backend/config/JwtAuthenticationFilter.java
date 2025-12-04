package com.trm.roadmate_backend.config;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    // 생성자 주입
    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // 1. 인증 불필요 경로 (로그인/회원가입)는 필터 검증을 건너뛰고 바로 진행
        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/signup")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        String email = null;
        String token = null;

        // 2. Authorization 헤더 확인 및 토큰 추출
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // "Bearer " 제거
            if (jwtUtil.validateToken(token)) {
                email = jwtUtil.extractEmail(token); // 토큰에서 이메일 추출
            }
        } else {
            // 토큰이 없으면 인증 처리 없이 다음 필터로 진행
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 이메일이 유효하고, Security Context에 인증 정보가 없는 경우에만 인증 처리
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
}
