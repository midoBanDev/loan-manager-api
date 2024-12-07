package com.gt.auth.application.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gt.auth.api.dto.AuthRequest;
import com.gt.auth.api.dto.AuthResponse;
import com.gt.auth.infra.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    
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
                .expiresIn(jwtTokenProvider.getTokenExpirationInSeconds())
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        // 리프레시 토큰 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        // 사용자 정보 추출
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        
        // 새로운 액세스 토큰 생성
        String newAccessToken = jwtTokenProvider.createToken(username, "ROLE_USER"); // 실제 구현시 사용자의 실제 권한을 가져와야 함
        String newRefreshToken = jwtTokenProvider.createRefreshToken(username);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getTokenExpirationInSeconds())
                .build();
    }
} 