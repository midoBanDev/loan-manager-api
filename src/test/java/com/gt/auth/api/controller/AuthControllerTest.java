package com.gt.auth.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.hamcrest.Matchers.containsString;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gt.auth.api.dto.AuthRequest;
import com.gt.user.domain.entity.User;
import com.gt.user.domain.entity.UserRole;
import com.gt.user.domain.repository.UserRepository;
import com.gt.global.config.security.OAuth2AuthenticationSuccessHandler;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Value("${app.oauth2.success-url}")
    private String successUrl;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        User user = new User(
            "test@example.com",
            passwordEncoder.encode("password123"),
            "testuser",
            UserRole.ADMIN
        );
        userRepository.save(user);
    }

    @Test
    @DisplayName("로그인 테스트 - 응답 내용 로깅")
    void login() throws Exception {
        // given
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        
        // when
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print()) // 요청/응답 전체 내용을 로그로 출력
                .andReturn();

        // then
        String responseBody = result.getResponse().getContentAsString();
        log.info("로그인 응답 내용: {}", responseBody);
    }

    @Test
    @DisplayName("로그인 성공 테스트 - 상세 검증")
    void loginSuccess() throws Exception {
        // given
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        // when & then
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andDo(print())
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        log.info("로그인 성공 응답 내용: {}", responseBody);
    }

    @Test
    @DisplayName("로그인 실패 테스트 - 잘못된 비밀번호")
    void loginFailWrongPassword() throws Exception {
        // given
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        // when & then
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andDo(print())
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        log.info("로그인 실패 응답 내용 (잘못된 비밀번호): {}", responseBody);
    }

    @Test
    @DisplayName("로그인 실패 테스트 - 존재하지 않는 사용자")
    void loginFailUserNotFound() throws Exception {
        // given
        AuthRequest request = new AuthRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        // when & then
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andDo(print())
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        log.info("로그인 실패 응답 내용 (존재하지 않는 사용자): {}", responseBody);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ClientRegistrationRepository clientRegistrationRepository() {
            return new InMemoryClientRegistrationRepository(
                CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId("")
                    .clientSecret("")
                    .build()
            );
        }
    }

    @Test
    @DisplayName("OAuth2 Google 로그인 성공 테스트")
    public void testOAuth2GoogleLogin() throws Exception {
        // Given
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "12345");
        attributes.put("email", "test@gmail.com");
        attributes.put("name", "Test User");
        attributes.put("picture", "https://example.com/photo.jpg");

        OAuth2User oauth2User = new DefaultOAuth2User(
            Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "sub"
        );

        // When & Then
        // 1. 인증 시작점 테스트
        MvcResult authorizationResult = mockMvc.perform(get("/oauth2/authorize/google")
                .with(oauth2Login().oauth2User(oauth2User)))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("http://localhost:8080/login/oauth2/code/google")))
                .andReturn();

        // 리다이렉션 URL 검증
        String redirectUrl = authorizationResult.getResponse().getHeader("Location");
        Assertions.assertThat(redirectUrl).contains("redirect_uri=http://localhost:8080/login/oauth2/code/google");
    }

    @Test
    @DisplayName("OAuth2 Google 로그인 실패 테스트")
    public void testOAuth2GoogleLoginFailure() throws Exception {
        // When & Then
        mockMvc.perform(get("/login/oauth2/code/google"))
                .andExpect(status().isUnauthorized());
    }
} 