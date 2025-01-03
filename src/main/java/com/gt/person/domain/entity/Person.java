package com.gt.person.domain.entity;

import com.gt.global.common.entity.BaseTimeEntity;
import com.gt.person.api.dto.PersonCreateRequest;
import com.gt.person.api.dto.PersonCreateResponse;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "person")
public class Person extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "person_id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "birth")
    private String birth;

    @Column(name = "gender")
    private String gender;

    @Column(name = "address1")
    private String address1;

    @Column(name = "address2")
    private String address2;

    @Builder
    public Person(String name, String phone, String birth, String gender, String address1, String address2) {
        this.name = name;
        this.phone = phone;
        this.birth = birth;
        this.gender = gender;
        this.address1 = address1;
        this.address2 = address2;
    }

    /**
     * 고객 정보 생성
     * @param request
     * @return
     */
    public static Person createPerson(PersonCreateRequest request){
        return Person.builder()
            .name(request.getName())
            .phone(request.getPhone())
            .birth(request.getBirth())
            .gender(request.getGender())
            .address1(request.getAddress1())
            .address2(request.getAddress2())
            .build();
    }

    public PersonCreateResponse convertToCreateResponseDto(){
        return PersonCreateResponse.builder()
            .id(this.id)
            .build();
    }
}

