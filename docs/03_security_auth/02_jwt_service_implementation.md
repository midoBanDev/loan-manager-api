# JWT 서비스 구현 상세 가이드

## 1. JWT 속성 설정 상세 설명

### 1.1 JwtProperties 클래스
```java
@Getter
@ConfigurationProperties(prefix = "jwt")  // application.yml의 jwt 프로퍼티와 매핑
@Configuration
public class JwtProperties {
    
    private final String secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    
    public JwtProperties(
            @Value("${jwt.secret-key}") String secretKey,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        this.secretKey = secretKey;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }
}
```

**클래스 설명**:
- `@ConfigurationProperties`: application.yml의 jwt 프로퍼티를 자바 객체로 매핑
- `@Value`: 개별 프로퍼티 값을 필드에 주입
- 생성자 주입을 통한 불변성 보장

**프로퍼티 설명**:
- `secretKey`: JWT 서명에 사용할 비밀키 (최소 256비트 이상 권장)
- `accessTokenExpiration`: 액세스 토큰 만료 시간 (밀리초)
- `refreshTokenExpiration`: 리프레시 토큰 만료 시간 (밀리초)

## 2. JWT 서비스 상세 설명

### 2.1 JwtService 클래스
```java
@Service
@RequiredArgsConstructor
public class JwtService {
    
    private final JwtProperties jwtProperties;
    private final Key signingKey;
    
    @PostConstruct  // 빈 초기화 시 실행
    public void init() {
        // Base64로 인코딩된 비밀키를 디코딩하여 서명 키 생성
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecretKey());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }
    
    // 토큰에서 사용자명 추출
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    // 토큰 생성 메서드
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }
    
    // 추가 클레임을 포함한 토큰 생성
    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        return buildToken(extraClaims, userDetails, jwtProperties.getAccessTokenExpiration());
    }
    
    // 리프레시 토큰 생성
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, jwtProperties.getRefreshTokenExpiration());
    }
    
    // 토큰 유효성 검증
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }
}
```

**주요 메서드 설명**:

1. **초기화 메서드**
```java
@PostConstruct
public void init() {
    byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecretKey());
    this.signingKey = Keys.hmacShaKeyFor(keyBytes);
}
```
- `@PostConstruct`: 빈 생성 후 초기화 작업 수행
- `Decoders.BASE64.decode()`: Base64 인코딩된 비밀키를 바이트 배열로 변환
- `Keys.hmacShaKeyFor()`: HMAC-SHA 알고리즘용 서명 키 생성

2. **토큰 생성 메서드**
```java
private String buildToken(
        Map<String, Object> extraClaims,
        UserDetails userDetails,
        long expiration
) {
    return Jwts
        .builder()
        .setClaims(extraClaims)  // 추가 클레임 설정
        .setSubject(userDetails.getUsername())  // 사용자 식별자
        .setIssuedAt(new Date(System.currentTimeMillis()))  // 발급 시간
        .setExpiration(new Date(System.currentTimeMillis() + expiration))  // 만료 시간
        .signWith(signingKey, SignatureAlgorithm.HS256)  // 서명
        .compact();  // 토큰 생성
}
```
- `setClaims()`: 추가적인 클레임 정보 설정
- `setSubject()`: 토큰 주체(사용자 식별자) 설정
- `setIssuedAt()`: 토큰 발급 시간 설정
- `setExpiration()`: 토큰 만료 시간 설정
- `signWith()`: HS256 알고리즘으로 토큰 서명
- `compact()`: 최종 JWT 문자열 생성

3. **토큰 파싱 및 검증 메서드**
```java
private Claims extractAllClaims(String token) {
    return Jwts
        .parserBuilder()
        .setSigningKey(signingKey)  // 서명 키 설정
        .build()
        .parseClaimsJws(token)  // 토큰 파싱
        .getBody();  // 클레임 추출
}

private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
}
```
- `parserBuilder()`: JWT 파서 생성
- `setSigningKey()`: 토큰 검증용 서명 키 설정
- `parseClaimsJws()`: 서명된 JWT 파싱
- `claimsResolver`: 특정 클레임을 추출하는 함수형 인터페이스

## 3. 리프레시 토큰 저장소 상세 설명

### 3.1 RefreshToken 엔티티
```java
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String token;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false)
    private LocalDateTime expiryDate;
    
    @Builder
    public RefreshToken(String token, String username, LocalDateTime expiryDate) {
        this.token = token;
        this.username = username;
        this.expiryDate = expiryDate;
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}
```

**엔티티 설명**:
- `@Table`: 리프레시 토큰 테이블 매핑
- `token`: 실제 리프레시 토큰 값 (유니크 제약조건)
- `username`: 토큰 소유자의 식별자
- `expiryDate`: 토큰 만료 시간
- `isExpired()`: 토큰 만료 여부 확인 메서드

### 3.2 RefreshTokenService 상세 설명
```java
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    
    @Transactional
    public RefreshToken createRefreshToken(String username) {
        // 새로운 리프레시 토큰 생성
        RefreshToken refreshToken = RefreshToken.builder()
            .username(username)
            .token(UUID.randomUUID().toString())  // 유니크한 토큰 값 생성
            .expiryDate(LocalDateTime.now().plusMillis(
                jwtProperties.getRefreshTokenExpiration()
            ))
            .build();
        
        // 기존 토큰 삭제
        refreshTokenRepository.deleteByUsername(username);
        
        // 새 토큰 저장
        return refreshTokenRepository.save(refreshToken);
    }
    
    @Transactional
    public JwtTokenResponse refreshAccessToken(String refreshToken) {
        return findByToken(refreshToken)
            .filter(token -> !token.isExpired())  // 만료되지 않은 토큰만 처리
            .map(token -> {
                UserDetails userDetails = loadUserByUsername(token.getUsername());
                String accessToken = jwtService.generateToken(userDetails);
                
                return JwtTokenResponse.of(
                    accessToken,
                    refreshToken,
                    jwtProperties.getAccessTokenExpiration()
                );
            })
            .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));
    }
}
```

**주요 메서드 설명**:

1. **리프레시 토큰 생성**
```java
@Transactional
public RefreshToken createRefreshToken(String username) {
    RefreshToken refreshToken = RefreshToken.builder()
        .username(username)
        .token(UUID.randomUUID().toString())
        .expiryDate(LocalDateTime.now().plusMillis(
            jwtProperties.getRefreshTokenExpiration()
        ))
        .build();
    
    refreshTokenRepository.deleteByUsername(username);
    return refreshTokenRepository.save(refreshToken);
}
```
- `UUID.randomUUID()`: 유니크한 토큰 값 생성
- `deleteByUsername()`: 기존 토큰 삭제로 동시 사용 방지
- `@Transactional`: 토큰 삭제와 생성을 하나의 트랜잭션으로 처리

2. **액세스 토큰 갱신**
```java
@Transactional
public JwtTokenResponse refreshAccessToken(String refreshToken) {
    return findByToken(refreshToken)
        .filter(token -> !token.isExpired())
        .map(token -> {
            UserDetails userDetails = loadUserByUsername(token.getUsername());
            String accessToken = jwtService.generateToken(userDetails);
            
            return JwtTokenResponse.of(
                accessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpiration()
            );
        })
        .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));
}
```
- `filter()`: 만료된 토큰 필터링
- `map()`: 유효한 토큰에 대해 새로운 액세스 토큰 생성
- `orElseThrow()`: 유효하지 않은 토큰에 대한 예외 처리

## 4. 예외 처리 상세 설명

### 4.1 JWT 관련 예외 클래스
```java
public class JwtAuthenticationException extends AuthenticationException {
    public JwtAuthenticationException(String message) {
        super(message);
    }
}

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
```

**예외 클래스 설명**:
- `JwtAuthenticationException`: JWT 인증 과정에서 발생하는 예외
- `InvalidRefreshTokenException`: 리프레시 토큰 검증 실패 시 발생하는 예외

### 4.2 예외 처리기
```java
@RestControllerAdvice
public class JwtExceptionHandler {
    
    @ExceptionHandler(JwtAuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleJwtAuthenticationException(JwtAuthenticationException e) {
        return new ErrorResponse(
            "JWT_AUTHENTICATION_ERROR",
            e.getMessage()
        );
    }
    
    @ExceptionHandler(InvalidRefreshTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidRefreshTokenException(InvalidRefreshTokenException e) {
        return new ErrorResponse(
            "INVALID_REFRESH_TOKEN",
            e.getMessage()
        );
    }
}
```
**예외 처리기 설명**:
- `@RestControllerAdvice`: 전역 예외 처리기 설정
- `@ExceptionHandler`: 특정 예외 타입에 대한 처리 메서드 지정
- `@ResponseStatus`: HTTP 응답 상태 코드 설정
- `ErrorResponse`: 일관된 에러 응답 형식 제공
