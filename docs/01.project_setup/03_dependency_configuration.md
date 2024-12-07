# 의존성 설정 가이드

## 1. Gradle 기본 설정

### 1.1 build.gradle 기본 구조
```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.growthmate'
version = '0.0.1-SNAPSHOT'

java {
    sourceCompatibility = '17'
}
```

## 2. 핵심 의존성 설명

### 2.1 Spring Boot Starter
```gradle
dependencies {
    // Spring Web
    implementation 'org.springframework.boot:spring-boot-starter-web'
    // Spring Web MVC 프레임워크
    // 내장 Tomcat 서버
    // JSON 처리를 위한 Jackson 라이브러리

    // Spring Data JPA
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    // JPA와 Hibernate
    // 데이터베이스 연동
    // 트랜잭션 관리

    // Spring Security
    implementation 'org.springframework.boot:spring-boot-starter-security'
    // 인증/인가 프레임워크
    // 보안 필터
    // 암호화 유틸리티
}
```

### 2.2 데이터베이스
```gradle
dependencies {
    // PostgreSQL 드라이버
    runtimeOnly 'org.postgresql:postgresql'
    
    // 데이터베이스 마이그레이션
    implementation 'org.flywaydb:flyway-core'
}
```

### 2.3 유틸리티 및 도구
```gradle
dependencies {
    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    // 보일러플레이트 코드 제거
    // 로깅, 생성자, Getter/Setter 자동 생성

    // Validation
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    // 입력값 검증
    // Bean Validation API

    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.11.5'
    // JSON Web Token 생성 및 검증
}
```

### 2.4 개발 도구
```gradle
dependencies {
    // Spring Boot DevTools
    developmentOnly 'org.springframework.boot:spring-boot-devtools'
    // 자동 재시작
    // 라이브 리로드
    // 개발 환경 최적화

    // Spring Configuration Processor
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
    // 설정 프로퍼티 메타데이터 생성
}
```

### 2.5 테스트
```gradle
dependencies {
    // Spring Boot Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    // JUnit 5
    // Mockito
    // AssertJ
    // Spring Test

    // Spring Security Test
    testImplementation 'org.springframework.security:spring-security-test'
    // 보안 관련 테스트 지원

    // Test Containers
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
    // 통합 테스트를 위한 컨테이너 지원
}
```

## 3. 의존성 관리

### 3.1 버전 관리
```gradle
ext {
    set('springCloudVersion', "2023.0.0")
    set('testcontainersVersion', "1.19.3")
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
        mavenBom "org.testcontainers:testcontainers-bom:${testcontainersVersion}"
    }
}
```

### 3.2 의존성 제외
```gradle
dependencies {
    implementation('org.springframework.boot:spring-boot-starter-web') {
        exclude group: 'org.springframework.boot', module: 'spring-boot-starter-tomcat'
    }
    implementation 'org.springframework.boot:spring-boot-starter-undertow'
}
```

## 4. 빌드 설정

### 4.1 JAR 설정
```gradle
jar {
    enabled = false
}

bootJar {
    archiveFileName = 'app.jar'
    manifest {
        attributes 'Main-Class': 'com.growthmate.GrowthMateApplication'
    }
}
```

### 4.2 테스트 설정
```gradle
test {
    useJUnitPlatform()
    testLogging {
        events "passed", "skipped", "failed"
    }
}
```

## 5. 프로필별 설정

### 5.1 프로필 구성
- local: 로컬 개발 환경
- dev: 개발 서버 환경
- prod: 운영 환경

### 5.2 프로필별 프로퍼티
```yaml
# application-local.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/growthmate
    username: ${LOCAL_DB_USERNAME}
    password: ${LOCAL_DB_PASSWORD}

# application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://dev-db:5432/growthmate
    username: ${DEV_DB_USERNAME}
    password: ${DEV_DB_PASSWORD}

# application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://prod-db:5432/growthmate
    username: ${PROD_DB_USERNAME}
    password: ${PROD_DB_PASSWORD}
``` 