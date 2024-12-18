package com.gt.auth.infra.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.gt.auth.application.service.CustomUserDetailsService;
import com.gt.auth.application.service.TokenBlacklistService;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-seconds}")
    private long tokenValidityInSeconds;

    @Value("${jwt.refresh-token-validity-in-milliseconds}")
    private long refreshTokenValidityInMilliseconds;

    private SecretKey key;

    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    @PostConstruct
    protected void init() {
        String encodedKey = Base64.getEncoder().encodeToString(secretKey.getBytes(StandardCharsets.UTF_8));
        key = Keys.hmacShaKeyFor(encodedKey.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(String username, String roles) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + (tokenValidityInSeconds * 1000));

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(String username) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = extractAllClaims(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        log.info("bearerToken = {}", bearerToken);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            log.info("bearerToken.substring(7) = {}", bearerToken.substring(7));
            return bearerToken.substring(7);
        }
        return null;
    }

    public boolean validateToken(String token) {

        if(!StringUtils.hasText(token)) {
            return false;
        }


         // 1. 토큰이 블랙리스트에 있는지 확인
        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            log.warn("블랙리스트에 등록된 토큰입니다.");
            return false;
        }

        // 2. 토큰 서명 검증
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return extractAllClaims(token).getSubject();
    }

    public long getTokenExpirationInSeconds() {
        return tokenValidityInSeconds;
    }

    public long getRefreshTokenExpirationInSeconds() {
        return refreshTokenValidityInMilliseconds / 1000;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 토큰의 남은 유효시간을 초 단위로 반환
     * @param token JWT 토큰
     * @return 남은 유효시간(초), 만료된 경우 0 반환
     */
    public long getValidTimeExpirationFromToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            
            Date expiration = claims.getExpiration();
            long remainingTime = expiration.getTime() - System.currentTimeMillis();
            
            // 남은 시간이 음수인 경우(만료된 경우) 0 반환
            return Math.max(remainingTime / 1000, 0);
            
        } catch (JwtException | IllegalArgumentException e) {
            log.error("토큰 유효시간 확인 중 오류 발생: {}", e.getMessage());
            return 0;
        }
    }
} 