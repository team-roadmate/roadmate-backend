package com.trm.roadmate_backend.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // application.yml/properties에서 JWT 비밀 키 주입 (보안 강화)
    @Value("${jwt.secret}")
    private String SECRET_KEY;

    // application.yml/properties에서 ACCESS TOKEN 만료 시간 주입 (밀리초)
    @Value("${jwt.access.expiration}")
    private long ACCESS_EXPIRATION;

    // application.yml/properties에서 REFRESH TOKEN 만료 시간 주입 (밀리초)
    @Value("${jwt.refresh.expiration}")
    private long REFRESH_EXPIRATION;

    // HMAC SHA 키 생성을 위한 시그니처 키 반환
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    /**
     * 특정 사용자 이메일을 기반으로 ACCESS JWT 토큰을 생성합니다.
     * @param email 토큰에 담을 사용자 이메일 (Subject)
     * @return 생성된 액세스 토큰 JWT 문자열
     */
    public String generateAccessToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 특정 사용자 이메일을 기반으로 REFRESH JWT 토큰을 생성합니다.
     * 리프레시 토큰은 만료 시간만 길고, 일반적으로 클레임에 추가 정보를 담지 않습니다.
     * @param email 토큰에 담을 사용자 이메일 (Subject)
     * @return 생성된 리프레시 토큰 JWT 문자열
     */
    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 주어진 JWT 토큰에서 사용자 이메일(Subject)을 추출합니다.
     * @param token JWT 문자열
     * @return 추출된 사용자 이메일
     */
    public String extractEmail(String token) {
        // 기존과 동일
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * JWT 토큰의 유효성을 검사합니다. (서명, 만료일 검사)
     * @param token JWT 문자열
     * @return 유효하면 true, 아니면 false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 토큰 파싱 또는 검증 중 오류 발생 시 (만료, 변조 등)
            return false;
        }
    }
}