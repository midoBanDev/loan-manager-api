package com.gt.global.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


import lombok.extern.slf4j.Slf4j;

/**
 * CORS 설정
 * 적용 대상: Spring MVC가 처리하는 모든 HTTP 요청에 적용됩니다.
 * 목적: 일반적인 컨트롤러 레벨에서의 CORS 설정을 관리합니다.
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


        /**
         * Access-Control-Allow-Credentials 설정 주의:
         * 클라이언트에서 쿠키 또는 인증 정보를 포함하는 요청을 보내려면 allowedOrigins에 와일드카드(*)를 사용할 수 없습니다. 대신 특정 도메인을 명시해야 합니다.
         */
        registry.addMapping("/**") // 모든 경로에 대해 CORS 허용
                .allowedOrigins("http://"+originUrl+":"+originPort) // 프론트엔드 도메인 허용
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용할 HTTP 메서드
                .allowedHeaders("*"); // 허용할 헤더
                // .allowCredentials(true); // 쿠키 및 인증 정보 허용
    }

}
