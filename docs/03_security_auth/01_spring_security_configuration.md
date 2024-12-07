# Spring Security 설정 상세 가이드

## 1. 의존성 설정 상세 설명

### 1.1 Spring Security 의존성
```gradle
dependencies {
    // Spring Security 핵심 의존성
    implementation 'org.springframework.boot:spring-boot-starter-security'
    
    // JWT 관련 의존성
    implementation 'io.jsonwebtoken:jjwt-api:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.11.5'
    
    // 테스트를 위한 Security 의존성
    testImplementation 'org.springframework.security:spring-security-test'
}
```

**의존성 설명**:
- `spring-boot-starter-security`: Spring Security의 핵심 기능을 제공하는 스타터 패키지
  - 인증/인가 기능
  - 보안 필터 체인
  - 암호화 유틸리티
  - CSRF 보호 등

- `jjwt-api`: JWT 생성 및 검증을 위한 핵심 API
- `jjwt-impl`: JWT 구현체 (런타임에만 필요)
- `jjwt-jackson`: JWT JSON 직렬화/역직렬화 지원 (런타임에만 필요)

## 2. SecurityConfig 클래스 상세 설명

### 2.1 기본 설정
```java
@Configuration  // Spring 설정 클래스임을 나타냄
@EnableWebSecurity  // Spring Security 활성화
@RequiredArgsConstructor  // final 필드에 대한 생성자 주입
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authenticationProvider;
    private final CorsConfig corsConfig;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // SecurityFilterChain 구성
    }
}
```

**클래스 레벨 어노테이션 설명**:
- `@Configuration`: 이 클래스가 Spring의 설정 클래스임을 나타냅니다.
- `@EnableWebSecurity`: Spring Security를 활성화하고 웹 보안 설정을 가능하게 합니다.
- `@RequiredArgsConstructor`: final 필드에 대한 생성자를 자동으로 생성합니다.

**주입 필드 설명**:
- `jwtAuthenticationFilter`: JWT 토큰 기반 인증을 처리하는 필터
- `authenticationProvider`: 실제 인증 로직을 처리하는 제공자
- `corsConfig`: CORS 설정을 제공하는 설정 클래스

### 2.2 SecurityFilterChain 설정
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // CSRF 설정
        .csrf(csrf -> csrf.disable())  // REST API에서는 CSRF 보호가 필요없음
        
        // CORS 설정
        .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
        
        // 세션 설정
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        
        // 요청 URL별 접근 권한 설정
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/api/v1/auth/**",
                "/api/v1/public/**",
                "/swagger-ui/**",
                "/v3/api-docs/**"
            ).permitAll()
            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/v1/user/**").hasRole("USER")
            .anyRequest().authenticated()
        )
        
        // JWT 필터 설정
        .addFilterBefore(
            jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class
        )
        
        // 인증 제공자 설정
        .authenticationProvider(authenticationProvider);
        
    return http.build();
}
```

**SecurityFilterChain 설정 상세 설명**:

1. **CSRF 설정**
   - `csrf().disable()`: REST API에서는 CSRF 토큰이 필요없으므로 비활성화
   - CSRF는 웹 폼 기반의 전통적인 웹 애플리케이션에서 주로 사용

2. **CORS 설정**
   - `cors().configurationSource()`: CORS 정책을 설정
   - 외부 도메인에서의 API 접근을 제어

3. **세션 관리**
   - `sessionManagement().sessionCreationPolicy(STATELESS)`: 세션을 생성하지 않음
   - JWT를 사용하므로 서버에서 세션을 관리할 필요가 없음

4. **URL 접근 권한 설정**
   - `authorizeHttpRequests()`: URL 패턴별 접근 권한 설정
   - `permitAll()`: 인증 없이 접근 가능
   - `hasRole()`: 특정 역할을 가진 사용자만 접근 가능
   - `authenticated()`: 인증된 사용자만 접근 가능

5. **필터 설정**
   - `addFilterBefore()`: JWT 인증 필터를 기본 인증 필터 전에 추가
   - 토큰 기반 인증을 먼저 처리

6. **인증 제공자 설정**
   - `authenticationProvider()`: 커스텀 인증 제공자 설정
   - 실제 인증 로직을 처리하는 컴포넌트 지정

## 3. CORS 설정 상세 설명

### 3.1 CorsConfig 클래스
```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용할 Origin 설정
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",  // 개발 환경
            "https://growthmate.com"  // 운영 환경
        ));
        
        // 허용할 HTTP 메서드 설정
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));
        
        // 허용할 헤더 설정
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With"
        ));
        
        // 인증 정보 포함 설정
        configuration.setAllowCredentials(true);
        
        // 설정을 적용할 경로 지정
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }
}
```

**CORS 설정 상세 설명**:

1. **Origin 설정**
   - `setAllowedOrigins()`: 접근을 허용할 도메인 목록 설정
   - 개발 환경과 운영 환경의 도메인을 각각 지정

2. **HTTP 메서드 설정**
   - `setAllowedMethods()`: 허용할 HTTP 메서드 지정
   - RESTful API에 필요한 모든 메서드 포함

3. **헤더 설정**
   - `setAllowedHeaders()`: 허용할 HTTP 헤더 지정
   - 인증 및 콘텐츠 타입 관련 헤더 포함

4. **자격 증명 설정**
   - `setAllowCredentials(true)`: 쿠키 및 인증 헤더 포함 허용
   - 클라이언트의 인증 정보를 서버로 전송 가능

5. **경로 설정**
   - `registerCorsConfiguration()`: CORS 설정을 적용할 URL 패턴 지정
   - API 경로에 대해서만 CORS 정책 적용

## 4. 테스트 코드 상세 설명

### 4.1 SecurityConfig 테스트
```java
@SpringBootTest  // 스프링 부트 테스트 환경 구성
@AutoConfigureMockMvc  // MockMvc 자동 구성
class SecurityConfigTest {
    
    @Autowired
    private MockMvc mockMvc;  // HTTP 요청을 시뮬레이션하는 테스트 유틸리티
    
    @Test
    void whenPublicEndpoint_thenSuccess() throws Exception {
        // 공개 엔드포인트 테스트
        mockMvc.perform(get("/api/v1/public/health"))
            .andExpect(status().isOk());
    }
    
    @Test
    void whenPrivateEndpoint_thenUnauthorized() throws Exception {
        // 비인증 사용자의 보호된 엔드포인트 접근 테스트
        mockMvc.perform(get("/api/v1/user/profile"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @WithMockUser(roles = "USER")  // 테스트용 인증된 사용자 생성
    void whenAuthenticatedUser_thenSuccess() throws Exception {
        // 인증된 사용자의 보호된 엔드포인트 접근 테스트
        mockMvc.perform(get("/api/v1/user/profile"))
            .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser(roles = "USER")
    void whenUserAccessingAdminEndpoint_thenForbidden() throws Exception {
        // 권한이 없는 사용자의 관리자 엔드포인트 접근 테스트
        mockMvc.perform(get("/api/v1/admin/users"))
            .andExpect(status().isForbidden());
    }
}
```

**테스트 코드 상세 설명**:

1. **테스트 환경 설정**
   - `@SpringBootTest`: 전체 애플리케이션 컨텍스트를 로드하여 통합 테스트 수행
   - `@AutoConfigureMockMvc`: MockMvc를 자동으로 구성하여 HTTP 요청 테스트 가능

2. **공개 엔드포인트 테스트**
   - 인증이 필요없는 공개 API에 대한 접근 테스트
   - 200 OK 응답을 기대

3. **비인증 접근 테스트**
   - 인증되지 않은 사용자의 보호된 리소스 접근 테스트
   - 401 Unauthorized 응답을 기대

4. **인증된 사용자 테스트**
   - `@WithMockUser`: 테스트용 인증된 사용자 컨텍스트 생성
   - 적절한 권한을 가진 사용자의 접근 테스트

5. **권한 검사 테스트**
   - 부적절한 권한을 가진 사용자의 접근 테스트
   - 403 Forbidden 응답을 기대
