package com.ott.core.global.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    /**
     * [Fix #5] 토큰 타입 상수
     * Access Token과 Refresh Token을 구분하여 토큰 혼용 공격을 방지합니다.
     */
    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    protected void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Access Token 생성
     *
     * @param userId 사용자 ID (subject)
     * @param role   사용자 역할 (VIEWER, UPLOADER, ADMIN)
     * @return JWT Access Token 문자열
     */
    public String createAccessToken(Long userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_ACCESS)  // [Fix #5]
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Refresh Token 생성
     *
     * @param userId 사용자 ID
     * @return JWT Refresh Token 문자열
     */
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(userId.toString())
                .claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_REFRESH)  // [Fix #5]
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰에서 Authentication 객체 추출
     * SecurityContext에 저장할 인증 정보를 생성합니다.
     */
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        String userId = claims.getSubject();
        String role = claims.get("role", String.class);

        // Spring Security는 "ROLE_" prefix를 기대합니다
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

        return new UsernamePasswordAuthenticationToken(
                userId,                          // principal (userId를 String으로)
                null,                            // credentials
                Collections.singletonList(authority)  // authorities
        );
    }

    /**
     * 토큰 유효성 검증
     *
     * @return true: 유효, false: 만료/변조/잘못된 형식
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("지원하지 않는 JWT 토큰입니다: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("잘못된 JWT 토큰 형식입니다: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("JWT 서명이 유효하지 않습니다: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 비어있습니다: {}", e.getMessage());
        }
        return false;
    }

    /**
     * [Fix #5] Access Token인지 확인
     */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return TOKEN_TYPE_ACCESS.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * [Fix #5] Refresh Token인지 확인
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return TOKEN_TYPE_REFRESH.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public Long getUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}