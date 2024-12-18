package com.gt.auth.infra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        
        String path = request.getRequestURI(); 

        log.info("path = {}", path);

        if (path.startsWith("/swagger-ui/") || path.startsWith("/v3/api-docs/") 
            || path.startsWith("/api/v1/auth/social/google") || path.startsWith("/api/v1/auth/login")
            || path.startsWith("/api/v1/auth/refresh") || path.startsWith("/api/v1/auth/logout")) { 
            return true;
        }

        return false;
    }   


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        log.info("request.getRemoteAddr() = {}", request.getRemoteAddr());
                
        String token = jwtTokenProvider.resolveToken(request);

        log.info("token = {}", token);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            Authentication auth = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
} 