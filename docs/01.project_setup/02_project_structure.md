# 프로젝트 구조 가이드

## 1. 기본 디렉토리 구조

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── growthmate/
│   │           ├── config/        # 설정 클래스
│   │           ├── domain/        # 도메인 모델
│   │           ├── repository/    # 데이터 접근 계층
│   │           ├── service/       # 비즈니스 로직
│   │           ├── controller/    # API 엔드포인트
│   │           ├── security/      # 보안 관련 클래스
│   │           └── common/        # 공통 유틸리티
│   └── resources/
│       ├── application.yml       # 애플리케이션 설정
│       ├── static/              # 정적 리소스
│       └── templates/           # 템플릿 파일
└── test/
    └── java/
        └── com/
            └── growthmate/
                ├── controller/    # 컨트롤러 테스트
                ├── service/       # 서비스 테스트
                └── repository/    # 리포지토리 테스트
```

## 2. 주요 패키지 설명

### 2.1 config/
- 애플리케이션의 설정 클래스들이 위치
- Spring Security, JPA, Web 등의 설정
- Bean 설정 및 초기화

### 2.2 domain/
- 도메인 모델 (엔티티) 클래스
- 값 객체 (Value Object)
- 도메인 이벤트

### 2.3 repository/
- JPA 리포지토리 인터페이스
- 커스텀 리포지토리 구현
- 쿼리 메소드

### 2.4 service/
- 비즈니스 로직 구현
- 트랜잭션 관리
- 도메인 서비스

### 2.5 controller/
- REST API 엔드포인트
- 요청/응답 DTO
- 예외 처리

### 2.6 security/
- 인증/인가 관련 클래스
- JWT 관련 구현
- 보안 필터

### 2.7 common/
- 공통 예외 클래스
- 유틸리티 클래스
- 상수 정의

## 3. 리소스 디렉토리 설명

### 3.1 application.yml
```yaml
spring:
  profiles:
    active: local  # 활성 프로필 설정
  
  datasource:
    url: jdbc:postgresql://localhost:5432/growthmate
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: 86400000  # 24시간
```

### 3.2 static/
- CSS, JavaScript, 이미지 등 정적 파일
- 웹 리소스
- 파비콘

### 3.3 templates/
- 이메일 템플릿
- PDF 템플릿
- 기타 템플릿 파일

## 4. 테스트 디렉토리 구조

### 4.1 테스트 계층 구분
- 단위 테스트 (`*Test.java`)
- 통합 테스트 (`*IntegrationTest.java`)
- E2E 테스트 (`*E2ETest.java`)

### 4.2 테스트 리소스
- `test/resources/application-test.yml`
- 테스트용 데이터 파일
- 픽스처 파일

## 5. 빌드 설정 파일

### 5.1 build.gradle
- 프로젝트 의존성 관리
- 빌드 설정
- 플러그인 설정

### 5.2 settings.gradle
- 프로젝트 이름 설정
- 멀티 모듈 설정 (해당되는 경우)

## 6. 기타 설정 파일

### 6.1 .gitignore
- Git 버전 관리 제외 파일 설정
- 환경 변수 파일 제외
- 빌드 결과물 제외

### 6.2 README.md
- 프로젝트 소개
- 설치 및 실행 방법
- API 문서 링크
- 기여 가이드 