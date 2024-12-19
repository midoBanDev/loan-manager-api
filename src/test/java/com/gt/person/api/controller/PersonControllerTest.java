package com.gt.person.api.controller;

import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.gt.config.RestDocsTestSupport;
import com.gt.person.api.dto.PersonCreateRequest;

public class PersonControllerTest extends RestDocsTestSupport {

    @Test
    @Tag("integration")
    void createPersonIntegrationTest() throws Exception {
        // given
        PersonCreateRequest request = new PersonCreateRequest();
        request.setName("John Doe");
        request.setGender("Male");
        request.setPhone("1234567890");
        request.setBirth("1990-01-01");
        request.setAddress1("123 Main St");
        request.setAddress2("Apt 4B");
        // when & then
        this.mockMvc.perform(post("/api/person/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Person created successfully"))
            .andExpect(jsonPath("$.data").exists())
            .andDo(print());

    }

    @Test
    @Tag("restdocs")
    void createPersonRestDocsTest() throws Exception {
        // given
        PersonCreateRequest request = new PersonCreateRequest();
        request.setName("John Doe");
        request.setGender("Male");
        request.setPhone("1234567890");
        request.setBirth("1990-01-01");
        request.setAddress1("123 Main St");
        request.setAddress2("Apt 4B");

        // when & then
        this.mockMvc.perform(post("/api/person/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Person created successfully"))
            .andExpect(jsonPath("$.data").exists())
            .andDo(document("{class-name}/{method-name}",
                requestFields(
                    fieldWithPath("name").description("이름"),
                    fieldWithPath("gender").description("성별"),
                    fieldWithPath("phone").description("전화번호"),
                    fieldWithPath("birth").description("생년월일"),
                    fieldWithPath("address1").description("주소1"),
                    fieldWithPath("address2").description("주소2")
                ),
                responseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("message").description("응답 메시지"),
                    fieldWithPath("data").description("생성된 Person ID")
                )
            ));

    }
}
