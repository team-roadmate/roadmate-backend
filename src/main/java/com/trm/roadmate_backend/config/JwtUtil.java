package com.trm.roadmate_backend.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value; // 🌟 새로 추가

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // 🌟 application.properties에서 값을 주입 받도록 변경 (보안 강화)
    @Value("${jwt.secret}") // 예시: jwt.secret=MySuperSecretKeyForJWTGeneration123456789012345
    private String SECRET_KEY;

    // 🌟 application.properties에서 값을 주입 받도록 변경
    @Value("${jwt.expiration}")
    private long EXPIRATION; // 예시: jwt.expiration=3600000 (1시간)

    private Key getSigningKey() {
        // 🌟 Javax.crypto 대신 java.security.Key를 사용하기 위한 Keys.hmacShaKeyFor
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    // ... (generateToken, extractEmail, validateToken 메소드 내용은 동일)
    public String generateToken(String email) {
        // ... (토큰 생성 로직)
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
