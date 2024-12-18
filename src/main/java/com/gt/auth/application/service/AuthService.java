package com.gt.auth.application.service;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.naming.AuthenticationException;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.gt.auth.api.dto.AuthRequest;
import com.gt.auth.api.dto.AuthResponse;
import com.gt.auth.domain.exception.JwtAuthenticationException;
import com.gt.auth.infra.security.JwtTokenProvider;
import com.gt.user.application.service.UserService;
import com.gt.user.domain.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final UserService userService;
    
    public AuthResponse login(AuthRequest request) {
        // 인증 시도

        log.info("비밀번호 = {}", request.getPassword());
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

    /**
     * 로그아웃 처리
     * @param accessToken 액세스 토큰
     * @param refreshToken 리프레시 토큰
     */
    public void logout(String accessToken, String refreshToken) {
        
        // 1. Access Token 유효성 검증
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new JwtAuthenticationException("Invalid access token");
        }


        // 2. Access Token 블랙리스트 추가
        long accessTokenRemainingTime = jwtTokenProvider.getValidTimeExpirationFromToken(accessToken); 
        if (accessTokenRemainingTime > 0) {
            tokenBlacklistService.blacklistToken(accessToken, accessTokenRemainingTime / 1000);
        }

        // 3. Refresh Token이 제공된 경우에만 블랙리스트에 추가
        if (jwtTokenProvider.validateToken(refreshToken)) {
            // Refresh Token의 남은 유효기간을 계산
            long refreshTokenRemainingTime = jwtTokenProvider.getValidTimeExpirationFromToken(refreshToken);
            
            if (refreshTokenRemainingTime > 0) {
                // Refresh Token을 블랙리스트에 추가 (TTL 설정)
                tokenBlacklistService.blacklistToken(refreshToken, refreshTokenRemainingTime / 1000);
            }
        }
    }

    @Transactional
    public AuthResponse socialLogin(String provider, String tokenId) throws AuthenticationException, GeneralSecurityException, IOException {
        if ("google".equals(provider)) {
            return handleGoogleLogin(tokenId);
        }
        throw new AuthenticationException("지원하지 않는 소셜 로그인 제공자입니다.");
    }

    private AuthResponse handleGoogleLogin(String tokenId) throws AuthenticationException, GeneralSecurityException, IOException {
        // Google ID 토큰 검증
        GoogleIdToken idToken = googleIdTokenVerifier.verify(tokenId);
        if (idToken == null) {
            throw new AuthenticationException("유효하지 않은 Google 토큰입니다.");
        }

        // 사용자 정보 추출
        GoogleIdToken.Payload payload = idToken.getPayload();

        log.info("payload = {}", payload);

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        log.info("email = {}", email);
        log.info("name = {}", name);
        log.info("pictureUrl = {}", pictureUrl);    

        if(email == null) {
            throw new AuthenticationException("email 정보가 없습니다.");
        }

        // 사용자 정보 저장 또는 업데이트
        User user = userService.findOrCreateGoogleUser(email, name, pictureUrl);

        if(user == null) {
            throw new AuthenticationException("사용자 정보가 존재하지 않습니다.");
        }

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createToken(email, user.getRole().toString());
        String refreshToken = jwtTokenProvider.createRefreshToken(email);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(jwtTokenProvider.getTokenExpirationInSeconds())
                .refreshTokenExpiresIn(jwtTokenProvider.getRefreshTokenExpirationInSeconds())
                .build();
    }
} 