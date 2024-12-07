# 권한 관리 구현 상세 가이드

## 1. 권한 모델 상세 설명

### 1.1 Role 열거형
```java
public enum Role {
    USER("ROLE_USER", "일반 사용자"),
    ADMIN("ROLE_ADMIN", "관리자"),
    EXPERT("ROLE_EXPERT", "전문가");
    
    private final String key;
    private final String description;
    
    Role(String key, String description) {
        this.key = key;
        this.description = description;
    }
    
    public String getKey() {
        return key;
    }
    
    public String getDescription() {
        return description;
    }
}
```

**열거형 설명**:
- `key`: Spring Security에서 사용하는 권한 키 (ROLE_ 접두어 필수)
- `description`: 권한에 대한 설명
- 각 권한은 불변의 상수로 관리

**권한 종류**:
1. **USER**: 일반 사용자 권한
   - 기본적인 서비스 이용
   - 개인 정보 관리
   - 콘텐츠 조회

2. **ADMIN**: 관리자 권한
   - 사용자 관리
   - 시스템 설정
   - 모든 기능 접근 가능

3. **EXPERT**: 전문가 권한
   - 전문가 콘텐츠 관리
   - 멘토링 서비스 제공
   - 특별 기능 접근 가능

### 1.2 UserRole 엔티티
```java
@Entity
@Table(name = "user_roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRole {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    
    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;
    
    @Column(name = "granted_by")
    private String grantedBy;
    
    @Builder
    public UserRole(User user, Role role, String grantedBy) {
        this.user = user;
        this.role = role;
        this.grantedAt = LocalDateTime.now();
        this.grantedBy = grantedBy;
    }
}
```

**엔티티 설명**:
- `@ManyToOne`: 사용자와의 다대일 관계 설정
- `fetch = FetchType.LAZY`: 지연 로딩으로 성능 최적화
- `@Enumerated(EnumType.STRING)`: Role 열거형을 문자열로 저장
- `grantedAt`: 권한 부여 시점 기록
- `grantedBy`: 권한 부여자 정보 기록

## 2. 권한 관리 서비스 상세 설명

### 2.1 RoleService 구현
```java
@Service
@RequiredArgsConstructor
@Transactional
public class RoleService {
    
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    
    public void grantRole(Long userId, Role role, String grantedBy) {
        // 사용자 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다"));
            
        // 권한 중복 검사
        if (userRoleRepository.existsByUserAndRole(user, role)) {
            throw new RoleAlreadyExistsException("이미 해당 권한이 존재합니다");
        }
        
        // 권한 부여
        UserRole userRole = UserRole.builder()
            .user(user)
            .role(role)
            .grantedBy(grantedBy)
            .build();
            
        userRoleRepository.save(userRole);
    }
    
    public void revokeRole(Long userId, Role role) {
        // 사용자 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다"));
            
        // 권한 조회 및 삭제
        UserRole userRole = userRoleRepository.findByUserAndRole(user, role)
            .orElseThrow(() -> new RoleNotFoundException("해당 권한이 존재하지 않습니다"));
            
        userRoleRepository.delete(userRole);
    }
    
    @Transactional(readOnly = true)
    public Set<Role> getUserRoles(Long userId) {
        return userRoleRepository.findByUserId(userId)
            .stream()
            .map(UserRole::getRole)
            .collect(Collectors.toSet());
    }
}
```

**서비스 메서드 설명**:

1. **권한 부여 (`grantRole`)**
   - 사용자 존재 여부 확인
   - 권한 중복 검사
   - 권한 부여 이력 기록
   - 트랜잭션 처리

2. **권한 회수 (`revokeRole`)**
   - 사용자 및 권한 존재 여부 확인
   - 권한 삭제
   - 트랜잭션 처리

3. **권한 조회 (`getUserRoles`)**
   - 읽기 전용 트랜잭션으로 성능 최적화
   - 사용자의 모든 권한을 Set으로 반환

## 3. 권한 검사 어노테이션 상세 설명

### 3.1 커스텀 권한 어노테이션
```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN')")
public @interface AdminOnly {
}

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('EXPERT')")
public @interface ExpertOnly {
}

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAnyRole('ADMIN', 'EXPERT')")
public @interface AdminOrExpertOnly {
}
```

**어노테이션 설명**:
- `@Target`: 어노테이션 적용 대상 지정
- `@Retention`: 어노테이션 유지 정책 설정
- `@PreAuthorize`: Spring Security의 메서드 수준 보안 적용

**사용 예시**:
```java
@AdminOnly
public void adminMethod() {
    // 관리자만 접근 가능한 메서드
}

@ExpertOnly
public void expertMethod() {
    // 전문가만 접근 가능한 메서드
}

@AdminOrExpertOnly
public void privilegedMethod() {
    // 관리자 또는 전문가만 접근 가능한 메서드
}
```

### 3.2 메서드 보안 설정
```java
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
    
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(new CustomPermissionEvaluator());
        return expressionHandler;
    }
}
```

**설정 설명**:
- `@EnableMethodSecurity`: 메서드 수준 보안 활성화
- `MethodSecurityExpressionHandler`: 권한 검사 표현식 처리기 설정
- `CustomPermissionEvaluator`: 커스텀 권한 평가 로직 구현

### 3.3 커스텀 권한 평가자
```java
public class CustomPermissionEvaluator implements PermissionEvaluator {
    
    @Override
    public boolean hasPermission(
            Authentication authentication,
            Object targetDomainObject,
            Object permission
    ) {
        if (authentication == null || targetDomainObject == null || !(permission instanceof String)) {
            return false;
        }
        
        String targetType = targetDomainObject.getClass().getSimpleName().toUpperCase();
        
        return hasPrivilege(authentication, targetType, permission.toString().toUpperCase());
    }
    
    @Override
    public boolean hasPermission(
            Authentication authentication,
            Serializable targetId,
            String targetType,
            Object permission
    ) {
        if (authentication == null || targetType == null || !(permission instanceof String)) {
            return false;
        }
        
        return hasPrivilege(authentication, targetType.toUpperCase(),
            permission.toString().toUpperCase());
    }
    
    private boolean hasPrivilege(Authentication auth, String targetType, String permission) {
        for (GrantedAuthority grantedAuth : auth.getAuthorities()) {
            if (grantedAuth.getAuthority().startsWith(targetType) &&
                grantedAuth.getAuthority().contains(permission)) {
                return true;
            }
        }
        return false;
    }
}
```

**권한 평가 로직 설명**:
1. **매개변수 검증**
   - 필수 매개변수 null 체크
   - 타입 안전성 검사

2. **권한 확인**
   - 대상 객체의 타입 추출
   - 권한 문자열 정규화
   - 권한 일치 여부 확인

3. **세부 권한 검사**
   - 객체 타입별 권한 검사
   - 계층적 권한 구조 지원
   - 유연한 권한 확장 가능

## 4. 권한 관리 컨트롤러 상세 설명

### 4.1 RoleController 구현
```java
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@AdminOnly
public class RoleController {
    
    private final RoleService roleService;
    
    @PostMapping("/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void grantRole(
            @PathVariable Long userId,
            @Valid @RequestBody RoleGrantRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        roleService.grantRole(userId, request.getRole(), userDetails.getUsername());
    }
    
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeRole(
            @PathVariable Long userId,
            @Valid @RequestBody RoleRevokeRequest request
    ) {
        roleService.revokeRole(userId, request.getRole());
    }
    
    @GetMapping("/{userId}")
    public RoleResponse getUserRoles(@PathVariable Long userId) {
        Set<Role> roles = roleService.getUserRoles(userId);
        return new RoleResponse(roles);
    }
}
```

**컨트롤러 설명**:
1. **클래스 레벨 설정**
   - `@AdminOnly`: 관리자만 접근 가능
   - `/api/v1/admin/roles` 기본 경로

2. **엔드포인트 설명**
   - 권한 부여: POST `/{userId}`
   - 권한 회수: DELETE `/{userId}`
   - 권한 조회: GET `/{userId}`

3. **보안 처리**
   - 관리자 권한 검사
   - 요청 데이터 유효성 검증
   - 감사 로그를 위한 수행자 정보 기록

## 5. 테스트 코드 상세 설명

### 5.1 RoleService 테스트
```java
@SpringBootTest
class RoleServiceTest {
    
    @Autowired
    private RoleService roleService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void whenGrantRole_thenSuccess() {
        // given
        User user = createTestUser();
        
        // when
        roleService.grantRole(user.getId(), Role.EXPERT, "admin@example.com");
        
        // then
        Set<Role> roles = roleService.getUserRoles(user.getId());
        assertTrue(roles.contains(Role.EXPERT));
    }
    
    @Test
    void whenGrantDuplicateRole_thenThrowException() {
        // given
        User user = createTestUser();
        roleService.grantRole(user.getId(), Role.EXPERT, "admin@example.com");
        
        // when & then
        assertThrows(RoleAlreadyExistsException.class, () -> {
            roleService.grantRole(user.getId(), Role.EXPERT, "admin@example.com");
        });
    }
}
```

**테스트 설명**:
1. **성공 케이스 테스트**
   - 테스트 사용자 생성
   - 권한 부여 수행
   - 권한 부여 결과 검증

2. **실패 케이스 테스트**
   - 중복 권한 부여 시도
   - 예외 발생 확인
   - 예외 메시지 검증 