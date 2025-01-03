package com.gt.auth.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gt.auth.api.dto.AuthRequest;
import com.gt.config.RestDocsTestSupport;
import com.gt.user.domain.entity.User;
import com.gt.user.domain.entity.UserRole;

import lombok.extern.slf4j.Slf4j;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;



@Slf4j
public class AuthControllerTest extends RestDocsTestSupport {

    @BeforeEach
    void setUp() {
        User user = User.builder()
            .name("test")
            .email("test@example.com")
            .password(new BCryptPasswordEncoder().encode("password123"))
            .role(UserRole.ADMIN)
            .provider("google")
            .build();
        userRepository.save(user);
    }


    @Test
    @Tag("restdocs")
    @DisplayName("로그인 성공 테스트")
    void login() throws Exception {
        // given
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        // when & then
        this.mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andDo(
                document("{class-name}/{method-name}",
                requestFields(
                    fieldWithPath("email").description("사용자 이메일"),
                    fieldWithPath("password").description("사용자 비밀번호")
                ),
                responseFields(
                    fieldWithPath("accessToken").description("JWT 액세스 토큰"),
                    fieldWithPath("refreshToken").description("JWT 리프레시 토큰"),
                    fieldWithPath("tokenType").description("토큰 타입"),
                    fieldWithPath("accessTokenExpiresIn").description("엑세스 토큰 만료 시간"),
                    fieldWithPath("refreshTokenExpiresIn").description("리프레시 토큰 만료 시간")
                )
            ));
    }
} 