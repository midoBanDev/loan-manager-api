package com.gt.global.config.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.google.api.client.util.Value;

import lombok.extern.slf4j.Slf4j;

/**
 * CORS 설정
 */
@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Value("${cors.origin-url}")
    private String originUrl;

    @Value("${cors.origin-port}")
    private String originPort;

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        log.info("originUrl = {}", originUrl);
        log.info("originPort = {}", originPort);    
        log.info("http://"+originUrl+":"+originPort);


        registry.addMapping("/**") // 모든 경로에 대해 CORS 허용
                .allowedOrigins("http://"+originUrl+":"+originPort) // 프론트엔드 도메인 허용
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용할 HTTP 메서드
                .allowedHeaders("*"); // 허용할 헤더
                // .allowCredentials(true); // 쿠키 및 인증 정보 허용
    }

}
