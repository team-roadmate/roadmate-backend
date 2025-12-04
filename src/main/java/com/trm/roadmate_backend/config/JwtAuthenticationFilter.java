package com.trm.roadmate_backend.config;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import io.jsonwebtoken.JwtException; // 추가

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

        // 1. 인증 불필요 경로 (로그인/회원가입/토큰 재발급)는 필터 검증을 건너뛰고 바로 진행
        // /api/auth/refresh 도 필터를 건너뛰어야 합니다.
        if (path.startsWith("/api/auth/")) { // 로그인/회원가입/재발급
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
                // 토큰 만료 또는 변조된 경우 (Access Token의 만료는 정상적인 흐름)
                // 클라이언트는 401 응답을 받고, Refresh Token을 사용하여 재발급을 시도해야 함.
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
                response.getWriter().write("Access token is invalid or expired.");
                return; // 필터 체인 진행 중단
            }

        } else {
            // 토큰이 없으면 다음 필터로 진행 (인증이 필요 없는 리소스에 접근할 경우를 대비)
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
}
