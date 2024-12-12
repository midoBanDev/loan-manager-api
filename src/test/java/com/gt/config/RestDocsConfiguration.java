package com.gt.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.operation.preprocess.Preprocessors;

@TestConfiguration
public class RestDocsConfiguration {

    @Bean
    public RestDocumentationResultHandler write() {
        return MockMvcRestDocumentation.document(
            "{class-name}/{method-name}",  // 문서 조각이 생성될 디렉토리 경로
            Preprocessors.preprocessRequest(
                Preprocessors.prettyPrint()  // 요청 본문을 보기 좋게 출력
            ),
            Preprocessors.preprocessResponse(
                Preprocessors.prettyPrint()  // 응답 본문을 보기 좋게 출력
            )
        );
    }
}
