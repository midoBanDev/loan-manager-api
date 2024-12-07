# 인증 컨트롤러 구현 상세 가이드

## 1. 인증 요청/응답 DTO 상세 설명

### 1.1 로그인 요청 DTO
```java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA 엔티티 생성을 위한 기본 생성자
public class LoginRequest {
    
    @NotBlank(message = "이메일은 필수 입력값입니다")  // null, 빈 문자열, 공백만 있는 문자열 불가
    @Email(message = "올바른 이메일 형식이 아닙니다")  // 이메일 형식 검증
    private String email;
    
    @NotBlank(message = "비밀번호는 필수 입력값입니다")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")  // 문자열 길이 검증
    private String password;
    
    @Builder  // 빌더 패턴 적용
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
```

**클래스 설명**:
- `@NoArgsConstructor`: JPA 엔티티 생성을 위한 기본 생성자 제공
- `@Getter`: 필드에 대한 getter 메서드 자동 생성
- `@Builder`: 객체 생성을 위한 빌더 패턴 적용

**유효성 검증 어노테이션**:
- `@NotBlank`: null, 빈 문자열, 공백만 있는 문자열 불허
- `@Email`: 이메일 형식 검증
- `@Size`: 문자열 길이 제한

### 1.2 회원가입 요청 DTO
```java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SignupRequest {
    
    @NotBlank(message = "이메일은 필수 입력값입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;
    
    @NotBlank(message = "비밀번호는 필수 입력값입니다")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
        message = "비밀번호는 8자 이상, 영문, 숫자, 특수문자를 포함해야 합니다"
    )
    private String password;
    
    @NotBlank(message = "닉네임은 필수 입력값입니다")
    @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하여야 합니다")
    private String nickname;
}
```

**비밀번호 정규식 설명** (`@Pattern`):
- `^`: 문자열 시작
- `(?=.*[A-Za-z])`: 최소 1개의 영문자
- `(?=.*\\d)`: 최소 1개의 숫자
- `(?=.*[@$!%*#?&])`: 최소 1개의 특수문자
- `[A-Za-z\\d@$!%*#?&]{8,}`: 허용된 문자들로 구성된 8자 이상의 문자열
- `$`: 문자열 끝

## 2. 인증 서비스 상세 설명

### 2.1 AuthenticationService 구현
```java
@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    
    public JwtTokenResponse signup(SignupRequest request) {
        // 이메일 중복 검사
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("이미 사용 중인 이메일입니다");
        }
        
        // 사용자 생성
        User user = User.builder()
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .nickname(request.getNickname())
            .roles(Set.of(Role.USER))
            .build();
        
        userRepository.save(user);
        
        // 토큰 생성
        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());
        
        return JwtTokenResponse.of(
            accessToken,
            refreshToken.getToken(),
            jwtProperties.getAccessTokenExpiration()
        );
    }
}
```

**회원가입 프로세스 설명**:
1. **이메일 중복 검사**
   - `existsByEmail()`: 이메일 중복 여부 확인
   - 중복 시 `EmailAlreadyExistsException` 발생

2. **사용자 생성**
   - 비밀번호 암호화: `passwordEncoder.encode()`
   - 기본 권한 부여: `Role.USER`
   - 빌더 패턴을 통한 객체 생성

3. **토큰 발급**
   - 액세스 토큰 생성: `jwtService.generateToken()`
   - 리프레시 토큰 생성: `refreshTokenService.createRefreshToken()`

### 2.2 로그인 처리
```java
public JwtTokenResponse login(LoginRequest request) {
    // 인증
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.getEmail(),
            request.getPassword()
        )
    );
    
    SecurityContextHolder.getContext().setAuthentication(authentication);
    
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    
    // 토큰 생성
    String accessToken = jwtService.generateToken(userDetails);
    RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername());
    
    return JwtTokenResponse.of(
        accessToken,
        refreshToken.getToken(),
        jwtProperties.getAccessTokenExpiration()
    );
}
```

**로그인 프로세스 설명**:
1. **인증 처리**
   - `UsernamePasswordAuthenticationToken` 생성
   - `authenticationManager.authenticate()`: 실제 인증 수행
   - 인증 실패 시 `AuthenticationException` 발생

2. **인증 컨텍스트 설정**
   - `SecurityContextHolder`: 현재 스레드의 보안 컨텍스트 관리
   - 인증된 사용자 정보 저장

3. **토큰 발급**
   - 인증된 사용자 정보로 토큰 생성
   - 액세스 토큰과 리프레시 토큰 발급

## 3. 인증 컨트롤러 상세 설명

### 3.1 AuthenticationController 구현
```java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    
    private final AuthenticationService authService;
    
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public JwtTokenResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }
    
    @PostMapping("/login")
    public JwtTokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
    
    @PostMapping("/refresh")
    public JwtTokenResponse refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        return authService.refreshToken(request);
    }
    
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal UserDetails userDetails) {
        authService.logout(userDetails.getUsername());
    }
}
```

**컨트롤러 어노테이션 설명**:
- `@RestController`: REST API 컨트롤러 선언
- `@RequestMapping`: 기본 URL 경로 설정
- `@Valid`: 요청 DTO의 유효성 검증 활성화
- `@ResponseStatus`: HTTP 응답 상태 코드 지정

**엔드포인트 설명**:
1. **회원가입 (`/signup`)**
   - HTTP Method: POST
   - 응답 상태: 201 Created
   - 요청 본문: SignupRequest
   - 응답 본문: JwtTokenResponse

2. **로그인 (`/login`)**
   - HTTP Method: POST
   - 응답 상태: 200 OK
   - 요청 본문: LoginRequest
   - 응답 본문: JwtTokenResponse

3. **토큰 갱신 (`/refresh`)**
   - HTTP Method: POST
   - 응답 상태: 200 OK
   - 요청 본문: TokenRefreshRequest
   - 응답 본문: JwtTokenResponse

4. **로그아웃 (`/logout`)**
   - HTTP Method: POST
   - 응답 상태: 204 No Content
   - 인증 필요: @AuthenticationPrincipal

## 4. 예외 처리 상세 설명

### 4.1 인증 관련 예외 처리기
```java
@RestControllerAdvice
public class AuthenticationExceptionHandler {
    
    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleEmailAlreadyExistsException(EmailAlreadyExistsException e) {
        return new ErrorResponse(
            "EMAIL_ALREADY_EXISTS",
            e.getMessage()
        );
    }
    
    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidCredentialsException(InvalidCredentialsException e) {
        return new ErrorResponse(
            "INVALID_CREDENTIALS",
            e.getMessage()
        );
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
            
        return new ErrorResponse(
            "VALIDATION_ERROR",
            message
        );
    }
}
```

**예외 처리 설명**:
1. **이메일 중복 예외**
   - 상태 코드: 409 Conflict
   - 에러 코드: EMAIL_ALREADY_EXISTS

2. **인증 실패 예외**
   - 상태 코드: 401 Unauthorized
   - 에러 코드: INVALID_CREDENTIALS

3. **유효성 검증 실패 예외**
   - 상태 코드: 400 Bad Request
   - 에러 코드: VALIDATION_ERROR
   - 모든 필드 에러 메시지를 결합하여 반환 
