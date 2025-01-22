package com.gt.global.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;

import com.gt.auth.infra.security.JwtAuthenticationFilter;
import com.gt.auth.infra.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Spring Security에서 설정된 CORS
     * 적용 대상: Spring Security가 보호하는 보안 필터 체인 내의 요청에만 적용됩니다.
     * 목적: 보안 필터를 통과하는 HTTP 요청에서 CORS를 검사하고, 허용 여부를 결정합니다.
     * @param http
     * @return
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            /**
             * http.cors()를 통해 Spring Security의 보안 필터 체인에서 CORS 설정을 활성화하면, Spring Security는 CORS 요청을 WebMvcConfigurer에 위임합니다.
             * 별도로 WebMvcConfigurer에 CORS를 설정하면 Spring Security 필터 체인 외의 요청에 대해서도 CORS를 검사하고 허용 여부를 결정합니다.
             * 또한 Spring Security에서 CORS를 직접 설정하지 않고 WebConfig에서 설정한 CORS를 대신 사용해도 처리되는 시점과 방식은 동일하다.
             */
            // 1. CORS 설정 (CorsFilter)
            .cors(cors -> {})   // WebMvcConfigurer로 CORS 위임
            
            /**
             * CSRF란 사용자가 모르게 악의적인 요청이 서버에 전달되도록 속이는 공격을 의미한다.
             * csrf를 활성화 하면 이런 공격에 대한 방어 매커니즘을 적용할 수 있습니다.
             * 하지만 JWT나 별도의 인증 토큰을 사용하는 경우 CSRF 방어가 필요하지 않습니다.
             * 만약 브라우저 쿠키를 사용한 세션 관리가 필요한 경우 CSRF 방어가 반드시 필요합니다.
             * 여기서는 JWT를 사용하는 경우이므로 CSRF 방어를 비활성화합니다.
             */
            // 2. CSRF 설정 (CsrfFilter)
            .csrf(csrf -> csrf.disable())
            
            /**
             * 세션 관리 설정
             * 세션 관리 설정은 세션 관리 필터를 통해 세션을 관리합니다.
             * 여기서는 세션을 비활성화하여 세션 관리를 비활성화합니다.
             */
            // 3. 세션 관리 설정 (SessionManagementFilter)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 4. JWT 필터 설정 (UsernamePasswordAuthenticationFilter 이전)
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), 
                UsernamePasswordAuthenticationFilter.class)
            
            // 5. 인증/인가 설정 (FilterSecurityInterceptor)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/auth/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/docs/**"
                ).permitAll()
                .anyRequest().authenticated())
            
            // 6. 예외 처리 설정 (ExceptionTranslationFilter)
            .exceptionHandling(exceptionHandling -> 
                exceptionHandling.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
} 