# GrowTogether Platform 현재 패키지 구조

## 프로젝트 기본 구조
```
gt-platform/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com.gt/
│   │   │       ├── auth/           # 인증/인가 관련
│   │   │       │   ├── api/        # REST API 컨트롤러
│   │   │       │   ├── application/# 서비스 계층
│   │   │       │   ├── domain/     # 도메인 모델
│   │   │       │   └── infra/      # 인프라스트럭처
│   │   │       │
│   │   │       ├── user/           # 사용자 관리
│   │   │       │   ├── api/
│   │   │       │   ├── application/
│   │   │       │   ├── domain/
│   │   │       │   └── infra/
│   │   │       │
│   │   │       ├── goal/           # 목표 관리
│   │   │       │   ├── api/
│   │   │       │   ├── application/
│   │   │       │   ├── domain/
│   │   │       │   └── infra/
│   │   │       │
│   │   │       ├── challenge/      # 챌린지
│   │   │       │   ├── api/
│   │   │       │   ├── application/
│   │   │       │   ├── domain/
│   │   │       │   └── infra/
│   │   │       │
│   │   │       ├── group/          # 그룹/커뮤니티
│   │   │       │   ├── api/
│   │   │       │   ├── application/
│   │   │       │   ├── domain/
│   │   │       │   └── infra/
│   │   │       │
│   │   │       ├── content/        # 콘텐츠 관리
│   │   │       │   ├── api/
│   │   │       │   ├── application/
│   │   │       │   ├── domain/
│   │   │       │   └── infra/
│   │   │       │
│   │   │       ├── reward/         # 보상/게이미피케이션
│   │   │       │   ├── api/
│   │   │       │   ├── application/
│   │   │       │   ├── domain/
│   │   │       │   └── infra/
│   │   │       │
│   │   │       ├── global/         # 공통 모듈
│   │   │       │   ├── config/     # 설정
│   │   │       │   ├── error/      # 예외 처리
│   │   │       │   └── util/       # 유틸리티
│   │   │       │
│   │   │       └── GtPlatformApplication.java
│   │   │
│   │   └── resources/             # 리소스 파일
│   │
│   └── test/                      # 테스트 코드
│
├── build.gradle                   # Gradle 빌드 설정
└── settings.gradle               # Gradle 프로젝트 설정
```

## 패키지 구조 설명

### 1. 도메인별 계층형 구조
각 도메인 패키지는 다음과 같은 4개의 하위 계층으로 구성됩니다:

- **api**: 외부와의 통신을 담당하는 컨트롤러 계층
- **application**: 비즈니스 로직을 처리하는 서비스 계층
- **domain**: 도메인 모델과 비즈니스 규칙
- **infra**: 외부 시스템과의 통합 및 기술적 구현

### 2. 주요 도메인 패키지
- **auth**: 인증/인가 관련 기능
- **user**: 사용자 관리 기능
- **goal**: 목표 설정 및 관리
- **challenge**: 챌린지 시스템
- **group**: 그룹 및 커뮤니티 기능
- **content**: 콘텐츠 관리
- **reward**: 보상 및 게이미피케이션

### 3. 공통 모듈 (global)
- **config**: 애플리케이션 설정
- **error**: 전역 예외 처리
- **util**: 공통 유틸리티

## 특징
1. **도메인 주도 설계(DDD) 적용**
   - 각 도메인이 독립적인 패키지로 분리
   - 도메인별로 동일한 계층 구조 유지

2. **계층형 아키텍처**
   - 각 도메인 내부가 계층으로 구분
   - 관심사의 명확한 분리

3. **모듈화**
   - 도메인별 독립적인 모듈화
   - 공통 기능의 global 패키지 분리