# JPA 엔티티 설계 상세 가이드

## 1. JPA 엔티티 설계 원칙

### 1.1 기본 원칙
1. **단일 책임 원칙**
   - 하나의 엔티티는 하나의 비즈니스 개념만 표현
   - 관련 없는 속성은 별도 엔티티로 분리
   - 응집도 높은 엔티티 설계

2. **불변성 유지**
   - 엔티티의 식별자는 변경 불가능하게 설계
   - 가능한 한 불변 객체로 설계
   - 변경이 필요한 경우 이력 관리 고려

3. **상속 관계 신중히 사용**
   - 상속보다 합성을 선호
   - 필요한 경우에만 상속 관계 사용
   - 구현 상속보다 인터페이스 상속 선호

### 1.2 명명 규칙
```java
// 클래스명: Pascal Case, 단수형
User, UserProfile, Goal

// 필드명: Camel Case
firstName, lastName, createdAt

// 연관관계 필드명: 관계를 명확히 표현
// 단수형 (1:1, N:1)
user, category
// 복수형 (1:N, N:M)
goals, members
```

## 2. 핵심 엔티티 구현

### 2.1 사용자 관련 엔티티
```java
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;
    
    @Column(nullable = false, unique = true)
    @Email
    private String email;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Column(nullable = false)
    @Size(min = 2, max = 50)
    private String nickname;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserProfile profile;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Goal> goals = new ArrayList<>();
    
    @Builder
    private User(String email, String passwordHash, String nickname) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
    }
    
    // 비즈니스 메서드
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }
}

@Entity
@Table(name = "user_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends BaseTimeEntity {
    
    @Id
    @Column(columnDefinition = "UUID")
    private UUID userId;
    
    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(columnDefinition = "TEXT")
    private String bio;
    
    @Column(name = "profile_image_url")
    private String profileImageUrl;
    
    @Column(name = "birth_date")
    private LocalDate birthDate;
    
    @Enumerated(EnumType.STRING)
    private Gender gender;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(name = "address_main")
    private String addressMain;
    
    @Column(name = "address_detail")
    private String addressDetail;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> notificationSettings = new HashMap<>();
    
    @Builder
    private UserProfile(User user) {
        this.user = user;
    }
    
    // 비즈니스 메서드
    public void updateBio(String bio) {
        this.bio = bio;
    }
    
    public void updateProfileImage(String imageUrl) {
        this.profileImageUrl = imageUrl;
    }
}
```

**엔티티 설명**:

1. **User 엔티티**
   - `@GeneratedValue(strategy = GenerationType.UUID)`: UUID 타입의 식별자 자동 생성
   - `@Email`: 이메일 형식 검증
   - `@Size`: 문자열 길이 제한
   - `@OneToOne`: UserProfile과 1:1 관계
   - `@OneToMany`: Goal과 1:N 관계

2. **UserProfile 엔티티**
   - `@MapsId`: User의 ID를 PK로 공유
   - `@Type(JsonType.class)`: JSONB 타입 매핑
   - `fetch = FetchType.LAZY`: 지연 로딩 설정

### 2.2 목표 관련 엔티티
```java
@Entity
@Table(name = "goal_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoalCategory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private GoalCategory parent;
    
    @OneToMany(mappedBy = "parent")
    private List<GoalCategory> children = new ArrayList<>();
    
    @Column(name = "display_order")
    private Integer displayOrder = 0;
    
    @Builder
    private GoalCategory(String name, String description, GoalCategory parent) {
        this.name = name;
        this.description = description;
        this.parent = parent;
    }
}

@Entity
@Table(name = "goals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Goal extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private GoalCategory category;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "target_value")
    private BigDecimal targetValue;
    
    @Column(name = "current_value")
    private BigDecimal currentValue = BigDecimal.ZERO;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalStatus status = GoalStatus.IN_PROGRESS;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility = Visibility.PUBLIC;
    
    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoalProgress> progresses = new ArrayList<>();
    
    @Builder
    private Goal(User user, GoalCategory category, String title, 
                LocalDate startDate, LocalDate endDate) {
        this.user = user;
        this.category = category;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    // 비즈니스 메서드
    public void updateProgress(BigDecimal value) {
        this.currentValue = value;
        if (currentValue.compareTo(targetValue) >= 0) {
            this.status = GoalStatus.COMPLETED;
        }
    }
    
    public void abandon() {
        this.status = GoalStatus.ABANDONED;
    }
}

@Entity
@Table(name = "goal_progress")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoalProgress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;
    
    @Column(name = "progress_date", nullable = false)
    private LocalDate progressDate;
    
    @Column(nullable = false)
    private BigDecimal value;
    
    @Column(columnDefinition = "TEXT")
    private String note;
    
    @Type(JsonType.class)
    @Column(name = "evidence_urls", columnDefinition = "jsonb")
    private List<String> evidenceUrls = new ArrayList<>();
    
    @Builder
    private GoalProgress(Goal goal, LocalDate progressDate, BigDecimal value) {
        this.goal = goal;
        this.progressDate = progressDate;
        this.value = value;
    }
}
```

**엔티티 설명**:

1. **GoalCategory 엔티티**
   - `@GeneratedValue(strategy = GenerationType.IDENTITY)`: 자동 증가 ID
   - 자기 참조 관계로 계층 구조 구현
   - `@OneToMany(mappedBy = "parent")`: 하위 카테고리 관리

2. **Goal 엔티티**
   - `@ManyToOne`: User, Category와 N:1 관계
   - `BigDecimal`: 정확한 수치 계산을 위해 사용
   - 비즈니스 로직이 포함된 메서드 구현

3. **GoalProgress 엔티티**
   - `@Type(JsonType.class)`: 증빙 URL 목록을 JSONB로 저장
   - `@ManyToOne`: Goal과 N:1 관계

### 2.3 커뮤니티 관련 엔티티
```java
@Entity
@Table(name = "groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Group extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private String category;
    
    @Column(name = "max_members")
    private Integer maxMembers;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility = Visibility.PUBLIC;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User creator;
    
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GroupMember> members = new HashSet<>();
    
    @Builder
    private Group(String name, String category, User creator) {
        this.name = name;
        this.category = category;
        this.creator = creator;
    }
    
    // 비즈니스 메서드
    public boolean canJoin() {
        return maxMembers == null || members.size() < maxMembers;
    }
    
    public void addMember(User user, GroupRole role) {
        if (!canJoin()) {
            throw new IllegalStateException("Group is full");
        }
        members.add(new GroupMember(this, user, role));
    }
}

@Entity
@Table(name = "group_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMember extends BaseTimeEntity {
    
    @EmbeddedId
    private GroupMemberId id;
    
    @MapsId("groupId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;
    
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupRole role = GroupRole.MEMBER;
    
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();
    
    @Builder
    private GroupMember(Group group, User user, GroupRole role) {
        this.id = new GroupMemberId(group.getId(), user.getId());
        this.group = group;
        this.user = user;
        this.role = role;
    }
}

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberId implements Serializable {
    
    @Column(name = "group_id", columnDefinition = "UUID")
    private UUID groupId;
    
    @Column(name = "user_id", columnDefinition = "UUID")
    private UUID userId;
}
```

**엔티티 설명**:

1. **Group 엔티티**
   - `Set<GroupMember>`: 중복 멤버 방지
   - 비즈니스 로직으로 멤버 추가 제어
   - `cascade = CascadeType.ALL`: 멤버 정보 함께 관리

2. **GroupMember 엔티티**
   - `@EmbeddedId`: 복합 키 사용
   - `@MapsId`: 복합 키의 각 필드를 매핑
   - `LocalDateTime`: 가입 시간 정확히 기록

## 3. 공통 컴포넌트

### 3.1 기본 엔티티
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseTimeEntity {
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

### 3.2 열거형 정의
```java
public enum UserStatus {
    ACTIVE, INACTIVE, SUSPENDED
}

public enum Gender {
    MALE, FEMALE, OTHER
}

public enum GoalStatus {
    NOT_STARTED, IN_PROGRESS, COMPLETED, ABANDONED
}

public enum Visibility {
    PUBLIC, FRIENDS, PRIVATE
}

public enum GroupRole {
    OWNER, ADMIN, MODERATOR, MEMBER
}
```

## 4. JPA 최적화 전략

### 4.1 N+1 문제 해결
```java
@Repository
public interface GoalRepository extends JpaRepository<Goal, UUID> {
    
    @Query("SELECT g FROM Goal g " +
           "LEFT JOIN FETCH g.category " +
           "LEFT JOIN FETCH g.progresses " +
           "WHERE g.user.id = :userId")
    List<Goal> findAllByUserIdWithDetails(@Param("userId") UUID userId);
    
    @EntityGraph(attributePaths = {"category", "progresses"})
    List<Goal> findAllByUserId(UUID userId);
}
```

### 4.2 벌크 연산 최적화
```java
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    @Modifying
    @Query("UPDATE User u SET u.status = :status " +
           "WHERE u.lastLoginAt < :date")
    int updateStatusForInactiveUsers(
        @Param("status") UserStatus status, 
        @Param("date") LocalDateTime date
    );
}
```

### 4.3 페이징 최적화
```java
@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {
    
    @Query(value = "SELECT g FROM Group g " +
                   "WHERE g.visibility = :visibility " +
                   "AND g.category = :category",
           countQuery = "SELECT COUNT(g) FROM Group g " +
                       "WHERE g.visibility = :visibility " +
                       "AND g.category = :category")
    Page<Group> findByVisibilityAndCategory(
        @Param("visibility") Visibility visibility,
        @Param("category") String category,
        Pageable pageable
    );
}
```

## 5. 성능 모니터링

### 5.1 SQL 로깅 설정
```yaml
spring:
  jpa:
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
        
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### 5.2 성능 측정 AOP
```java
@Aspect
@Component
@Slf4j
public class QueryPerformanceAspect {
    
    @Around("@annotation(QueryPerformance)")
    public Object measureQueryPerformance(ProceedingJoinPoint joinPoint) 
        throws Throwable {
        
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long endTime = System.currentTimeMillis();
        
        log.info("Method: {} - Execution Time: {}ms",
                joinPoint.getSignature().getName(),
                endTime - startTime);
        
        return result;
    }
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryPerformance {
}
``` 