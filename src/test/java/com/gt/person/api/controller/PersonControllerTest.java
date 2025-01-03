package com.gt.person.api.controller;

import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.assertj.core.api.Assertions;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.gt.config.RestDocsTestSupport;
import com.gt.person.api.dto.PersonCreateRequest;
import com.gt.person.domain.entity.Person;
import com.gt.person.domain.repository.PersonRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PersonControllerTest extends RestDocsTestSupport {

    @Autowired
    private PersonRepository personRepository;

    @Test
    @Tag("integration")
    void createPersonTest() throws Exception {
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
    @Tag("integration")
    void createPersonValidationTest() throws Exception {
        // given
        PersonCreateRequest request = new PersonCreateRequest();
        request.setName("");
        request.setGender("Male");
        request.setPhone("1234567890");
        request.setBirth("1990-01-01");
        request.setAddress1("123 Main St");
        request.setAddress2("Apt 4B");

        // when & then
        this.mockMvc.perform(post("/api/person/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest()) // 400 에러 예상
            .andDo(print());
    }
     
    @Test
    @Tag("integration")
    void auditingTest() throws Exception {
        // given
        PersonCreateRequest request = new PersonCreateRequest();
        request.setName("John Doe");
        request.setGender("Male");
        request.setPhone("1234567890");
        request.setBirth("1990-01-01");
        request.setAddress1("123 Main St");
        request.setAddress2("Apt 4B");

        // when
        this.mockMvc.perform(post("/api/person/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        // then
        Person person = personRepository.findByName(request.getName()).get();

        Assertions.assertThat(person.getCreatedDate()).isNotNull();
        Assertions.assertThat(person.getLastModifiedDate()).isNotNull();

        // 더티체킹
        person.setGender("Female");
        Person findPerson = personRepository.findByName(request.getName()).get();

        Assertions.assertThat(person.getLastModifiedDate()).isAfter(findPerson.getCreatedDate());
    }   


    @Test
    @Tag("restdocs")
    void createPerson() throws Exception {
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
