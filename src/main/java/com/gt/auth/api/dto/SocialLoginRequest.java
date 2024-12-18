package com.gt.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SocialLoginRequest {
    
    @NotBlank(message = "토큰 ID는 필수 입력값입니다.")
    private String idToken;
} 