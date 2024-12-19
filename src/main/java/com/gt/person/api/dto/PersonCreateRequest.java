package com.gt.person.api.dto;

import com.google.auto.value.AutoValue.Builder;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Builder
public class PersonCreateRequest {
    
    @NotBlank(message = "이름은 필수 입력 항목입니다.")
    private String name;    // 이름

    @NotBlank(message = "전화번호는 필수 입력 항목입니다.")
    private String phone;   // 전화번호

    @NotBlank(message = "생년월일은 필수 입력 항목입니다.")
    private String birth;   // 생년월일

    @NotBlank(message = "성별은 필수 입력 항목입니다.")
    private String gender;  // 성별

    @NotBlank(message = "주소1은 필수 입력 항목입니다.")
    private String address1; // 주소1

    private String address2; // 주소2
}
