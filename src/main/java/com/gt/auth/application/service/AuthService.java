package com.gt.auth.application.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gt.auth.api.dto.AuthRequest;
import com.gt.auth.api.dto.AuthResponse;
import com.gt.auth.domain.exception.JwtAuthenticationException;
import com.gt.auth.infra.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    
    public AuthResponse login(AuthRequest request) {
        // 인증 시도
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createToken(request.getEmail(), authentication.getAuthorities().toString());
        String refreshToken = jwtTokenProvider.createRefreshToken(request.getEmail());
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(jwtTokenProvider.getTokenExpirationInSeconds())
                .refreshTokenExpiresIn(jwtTokenProvider.getRefreshTokenExpirationInSeconds())
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        // 리프레시 토큰 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new JwtAuthenticationException("Invalid refresh token");
        }

        // 블랙리스트 확인
        if (tokenBlacklistService.isTokenBlacklisted(refreshToken)) {
            throw new JwtAuthenticationException("Refresh token has been blacklisted");
        }

        // 사용자 정보 추출
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        
        // 새로운 액세스 토큰 생성
        String newAccessToken = jwtTokenProvider.createToken(username, "ROLE_USER"); // 실제 구현시 사용자의 실제 권한을 가져와야 함
        String newRefreshToken = jwtTokenProvider.createRefreshToken(username);

        // 이전 리프레시 토큰 블랙리스트에 추가
        tokenBlacklistService.blacklistToken(refreshToken, jwtTokenProvider.getRefreshTokenExpirationInSeconds());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(jwtTokenProvider.getTokenExpirationInSeconds())
                .refreshTokenExpiresIn(jwtTokenProvider.getRefreshTokenExpirationInSeconds())
                .build();
    }

    public void logout(String accessToken) {
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new JwtAuthenticationException("Invalid access token");
        }

        // 토큰 블랙리스트에 추가
        tokenBlacklistService.blacklistToken(accessToken, jwtTokenProvider.getTokenExpirationInSeconds());
    }
} 