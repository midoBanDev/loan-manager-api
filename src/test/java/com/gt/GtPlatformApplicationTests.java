package com.gt;

import java.util.Arrays;

import org.springframework.context.ApplicationContext;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
class GtPlatformApplicationTests{

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        Assertions.assertThat(applicationContext).isNotNull();
        
        // 활성화된 프로파일 확인
        log.info("Active profiles: {}", 
            Arrays.toString(applicationContext.getEnvironment().getActiveProfiles()));
        
        // 로드된 빈 확인
        Arrays.stream(applicationContext.getBeanDefinitionNames())
              .forEach(log::info);
    }

}
