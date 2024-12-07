# 리포지토리 구현 상세 가이드

## 1. 리포지토리 설계 원칙

### 1.1 기본 원칙
1. **단일 책임 원칙**
   - 하나의 리포지토리는 하나의 엔티티만 담당
   - 연관된 엔티티의 조회는 별도 메서드로 구현
   - 복잡한 조회 로직은 QueryDSL 활용

2. **명명 규칙**
   - 리포지토리 인터페이스: `엔티티명Repository`
   - 커스텀 리포지토리: `엔티티명RepositoryCustom`
   - 구현체: `엔티티명RepositoryImpl`

3. **메서드 명명 규칙**
```java
// 조회
findById()              // ID로 단일 엔티티 조회
findAllBy...()         // 조건으로 목록 조회
findBy...And...()      // 다중 조건으로 조회
existsBy...()          // 조건으로 존재 여부 확인

// 수정/삭제
deleteBy...()          // 조건으로 삭제
updateBy...()          // 조건으로 수정
countBy...()           // 조건으로 개수 조회
```

## 2. 핵심 리포지토리 구현

### 2.1 사용자 관련 리포지토리
```java
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u " +
           "LEFT JOIN FETCH u.profile " +
           "WHERE u.id = :id")
    Optional<User> findByIdWithProfile(@Param("id") UUID id);
    
    @Query("SELECT u FROM User u " +
           "LEFT JOIN FETCH u.profile " +
           "LEFT JOIN FETCH u.goals " +
           "WHERE u.id = :id")
    Optional<User> findByIdWithProfileAndGoals(@Param("id") UUID id);
    
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.status = :status " +
           "WHERE u.id = :id")
    int updateStatus(@Param("id") UUID id, 
                    @Param("status") UserStatus status);
}

@Repository
public interface UserRepositoryCustom {
    List<UserSummaryDto> findActiveUsersWithGoalCount(LocalDateTime since);
    Page<UserDetailDto> searchUsers(UserSearchCondition condition, 
                                  Pageable pageable);
}

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {
    
    private final JPAQueryFactory queryFactory;
    
    @Override
    public List<UserSummaryDto> findActiveUsersWithGoalCount(
        LocalDateTime since) {
        
        return queryFactory
            .select(Projections.constructor(UserSummaryDto.class,
                QUser.user.id,
                QUser.user.email,
                QUser.user.nickname,
                QGoal.goal.count()))
            .from(QUser.user)
            .leftJoin(QUser.user.goals, QGoal.goal)
            .where(QUser.user.status.eq(UserStatus.ACTIVE)
                  .and(QUser.user.createdAt.goe(since)))
            .groupBy(QUser.user.id)
            .fetch();
    }
    
    @Override
    public Page<UserDetailDto> searchUsers(
        UserSearchCondition condition, 
        Pageable pageable) {
        
        JPAQuery<UserDetailDto> query = queryFactory
            .select(Projections.constructor(UserDetailDto.class,
                QUser.user.id,
                QUser.user.email,
                QUser.user.nickname,
                QUserProfile.userProfile.bio))
            .from(QUser.user)
            .leftJoin(QUser.user.profile, QUserProfile.userProfile)
            .where(
                emailLike(condition.getEmail()),
                nicknameLike(condition.getNickname()),
                statusEq(condition.getStatus())
            );
        
        long total = query.fetchCount();
        List<UserDetailDto> content = query
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
        
        return new PageImpl<>(content, pageable, total);
    }
    
    private BooleanExpression emailLike(String email) {
        return StringUtils.hasText(email) ? 
            QUser.user.email.like("%" + email + "%") : null;
    }
    
    private BooleanExpression nicknameLike(String nickname) {
        return StringUtils.hasText(nickname) ? 
            QUser.user.nickname.like("%" + nickname + "%") : null;
    }
    
    private BooleanExpression statusEq(UserStatus status) {
        return status != null ? 
            QUser.user.status.eq(status) : null;
    }
}
```

**리포지토리 설명**:

1. **UserRepository**
   - `findByIdWithProfile`: 프로필 정보 함께 조회
   - `findByIdWithProfileAndGoals`: 프로필과 목표 정보 함께 조회
   - `@Modifying(clearAutomatically = true)`: 벌크 연산 후 영속성 컨텍스트 초기화

2. **UserRepositoryCustom & Impl**
   - QueryDSL을 활용한 복잡한 조회 구현
   - 동적 쿼리 조건 처리
   - DTO 프로젝션으로 필요한 데이터만 조회

### 2.2 목표 관련 리포지토리
```java
@Repository
public interface GoalRepository extends JpaRepository<Goal, UUID>, 
                                     GoalRepositoryCustom {
    
    @EntityGraph(attributePaths = {"category", "user"})
    List<Goal> findAllByUserId(UUID userId);
    
    @Query("SELECT g FROM Goal g " +
           "LEFT JOIN FETCH g.category " +
           "LEFT JOIN FETCH g.progresses " +
           "WHERE g.user.id = :userId " +
           "AND g.status = :status")
    List<Goal> findAllByUserIdAndStatus(
        @Param("userId") UUID userId,
        @Param("status") GoalStatus status);
    
    @Query(value = "SELECT g FROM Goal g " +
                   "WHERE g.visibility = :visibility " +
                   "AND g.startDate <= :date " +
                   "AND g.endDate >= :date",
           countQuery = "SELECT COUNT(g) FROM Goal g " +
                       "WHERE g.visibility = :visibility " +
                       "AND g.startDate <= :date " +
                       "AND g.endDate >= :date")
    Page<Goal> findActivePublicGoals(
        @Param("visibility") Visibility visibility,
        @Param("date") LocalDate date,
        Pageable pageable);
    
    @Modifying
    @Query("UPDATE Goal g SET g.status = :status " +
           "WHERE g.endDate < :date " +
           "AND g.status = 'IN_PROGRESS'")
    int updateStatusForExpiredGoals(
        @Param("status") GoalStatus status,
        @Param("date") LocalDate date);
}

@Repository
public interface GoalRepositoryCustom {
    List<GoalSummaryDto> findGoalSummaries(GoalSearchCondition condition);
    Map<UUID, GoalProgressDto> findLatestProgress(List<UUID> goalIds);
}

@RequiredArgsConstructor
public class GoalRepositoryImpl implements GoalRepositoryCustom {
    
    private final JPAQueryFactory queryFactory;
    
    @Override
    public List<GoalSummaryDto> findGoalSummaries(
        GoalSearchCondition condition) {
        
        return queryFactory
            .select(Projections.constructor(GoalSummaryDto.class,
                QGoal.goal.id,
                QGoal.goal.title,
                QGoal.goal.status,
                QGoal.goal.startDate,
                QGoal.goal.endDate,
                QGoal.goal.currentValue,
                QGoal.goal.targetValue))
            .from(QGoal.goal)
            .where(
                userIdEq(condition.getUserId()),
                statusIn(condition.getStatuses()),
                dateRange(condition.getStartDate(), 
                         condition.getEndDate())
            )
            .orderBy(QGoal.goal.startDate.desc())
            .fetch();
    }
    
    @Override
    public Map<UUID, GoalProgressDto> findLatestProgress(
        List<UUID> goalIds) {
        
        List<GoalProgressDto> progressList = queryFactory
            .select(Projections.constructor(GoalProgressDto.class,
                QGoalProgress.goalProgress.goal.id,
                QGoalProgress.goalProgress.value,
                QGoalProgress.goalProgress.progressDate))
            .from(QGoalProgress.goalProgress)
            .where(QGoalProgress.goalProgress.goal.id.in(goalIds))
            .orderBy(QGoalProgress.goalProgress.progressDate.desc())
            .fetch();
        
        return progressList.stream()
            .collect(Collectors.toMap(
                GoalProgressDto::getGoalId,
                Function.identity(),
                (existing, replacement) -> existing
            ));
    }
    
    private BooleanExpression userIdEq(UUID userId) {
        return userId != null ? 
            QGoal.goal.user.id.eq(userId) : null;
    }
    
    private BooleanExpression statusIn(List<GoalStatus> statuses) {
        return statuses != null && !statuses.isEmpty() ? 
            QGoal.goal.status.in(statuses) : null;
    }
    
    private BooleanExpression dateRange(
        LocalDate startDate, 
        LocalDate endDate) {
        
        BooleanExpression result = null;
        
        if (startDate != null) {
            result = QGoal.goal.startDate.goe(startDate);
        }
        
        if (endDate != null) {
            BooleanExpression endDatePredicate = 
                QGoal.goal.endDate.loe(endDate);
            result = result != null ? 
                result.and(endDatePredicate) : endDatePredicate;
        }
        
        return result;
    }
}
```

**리포지토리 설명**:

1. **GoalRepository**
   - `@EntityGraph`: N+1 문제 해결을 위한 페치 조인
   - `findActivePublicGoals`: 페이징을 위한 카운트 쿼리 최적화
   - `updateStatusForExpiredGoals`: 만료된 목표 상태 일괄 업데이트

2. **GoalRepositoryCustom & Impl**
   - 복잡한 검색 조건 처리
   - 최신 진행상황 조회 최적화
   - 동적 쿼리 빌더 패턴 적용

### 2.3 그룹 관련 리포지토리
```java
@Repository
public interface GroupRepository extends JpaRepository<Group, UUID>, 
                                      GroupRepositoryCustom {
    
    @EntityGraph(attributePaths = {"members"})
    Optional<Group> findByIdWithMembers(UUID id);
    
    @Query("SELECT g FROM Group g " +
           "LEFT JOIN FETCH g.members m " +
           "LEFT JOIN FETCH m.user " +
           "WHERE g.id = :id")
    Optional<Group> findByIdWithMembersAndUsers(@Param("id") UUID id);
    
    @Query(value = "SELECT g FROM Group g " +
                   "WHERE g.visibility = :visibility " +
                   "AND g.category = :category " +
                   "AND SIZE(g.members) < g.maxMembers",
           countQuery = "SELECT COUNT(g) FROM Group g " +
                       "WHERE g.visibility = :visibility " +
                       "AND g.category = :category " +
                       "AND SIZE(g.members) < g.maxMembers")
    Page<Group> findAvailableGroups(
        @Param("visibility") Visibility visibility,
        @Param("category") String category,
        Pageable pageable);
}

@Repository
public interface GroupRepositoryCustom {
    List<GroupSummaryDto> findGroupSummaries(GroupSearchCondition condition);
    Map<UUID, GroupStatsDto> findGroupStats(List<UUID> groupIds);
}

@RequiredArgsConstructor
public class GroupRepositoryImpl implements GroupRepositoryCustom {
    
    private final JPAQueryFactory queryFactory;
    
    @Override
    public List<GroupSummaryDto> findGroupSummaries(
        GroupSearchCondition condition) {
        
        return queryFactory
            .select(Projections.constructor(GroupSummaryDto.class,
                QGroup.group.id,
                QGroup.group.name,
                QGroup.group.category,
                QGroup.group.visibility,
                QGroup.group.members.size()))
            .from(QGroup.group)
            .where(
                categoryEq(condition.getCategory()),
                visibilityIn(condition.getVisibilities()),
                hasAvailableSlots(condition.getOnlyAvailable())
            )
            .orderBy(QGroup.group.createdAt.desc())
            .fetch();
    }
    
    @Override
    public Map<UUID, GroupStatsDto> findGroupStats(List<UUID> groupIds) {
        List<GroupStatsDto> statsList = queryFactory
            .select(Projections.constructor(GroupStatsDto.class,
                QGroup.group.id,
                QGroup.group.members.size(),
                QGroupMember.groupMember.role.count(),
                QGoal.goal.count()))
            .from(QGroup.group)
            .leftJoin(QGroup.group.members, QGroupMember.groupMember)
            .leftJoin(QGroupMember.groupMember.user.goals, QGoal.goal)
            .where(QGroup.group.id.in(groupIds))
            .groupBy(QGroup.group.id)
            .fetch();
        
        return statsList.stream()
            .collect(Collectors.toMap(
                GroupStatsDto::getGroupId,
                Function.identity()
            ));
    }
    
    private BooleanExpression categoryEq(String category) {
        return StringUtils.hasText(category) ? 
            QGroup.group.category.eq(category) : null;
    }
    
    private BooleanExpression visibilityIn(List<Visibility> visibilities) {
        return visibilities != null && !visibilities.isEmpty() ? 
            QGroup.group.visibility.in(visibilities) : null;
    }
    
    private BooleanExpression hasAvailableSlots(Boolean onlyAvailable) {
        return Boolean.TRUE.equals(onlyAvailable) ? 
            QGroup.group.members.size()
                .lt(QGroup.group.maxMembers) : null;
    }
}
```

**리포지토리 설명**:

1. **GroupRepository**
   - `findByIdWithMembers`: 멤버 정보 포함 조회
   - `findByIdWithMembersAndUsers`: 멤버와 사용자 정보 포함 조회
   - `findAvailableGroups`: 가입 가능한 그룹 페이징 조회

2. **GroupRepositoryCustom & Impl**
   - 그룹 요약 정보 조회
   - 그룹 통계 정보 일괄 조회
   - 동적 조건 처리

## 3. 성능 최적화 전략

### 3.1 벌크 연산 최적화
```java
@Repository
public interface BulkOperationRepository {
    
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Goal g SET g.status = :status " +
           "WHERE g.endDate < CURRENT_DATE " +
           "AND g.status = 'IN_PROGRESS'")
    int updateExpiredGoals(@Param("status") GoalStatus status);
    
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM GoalProgress gp " +
           "WHERE gp.progressDate < :date")
    int deleteOldProgress(@Param("date") LocalDate date);
    
    @Query(value = 
        "INSERT INTO goal_progress (goal_id, progress_date, value) " +
        "SELECT g.id, CURRENT_DATE, g.current_value " +
        "FROM goals g " +
        "WHERE NOT EXISTS ( " +
        "    SELECT 1 FROM goal_progress gp " +
        "    WHERE gp.goal_id = g.id " +
        "    AND gp.progress_date = CURRENT_DATE" +
        ")", 
        nativeQuery = true)
    @Modifying
    int createDailyProgress();
}
```

### 3.2 배치 처리
```java
@Repository
public interface BatchRepository {
    
    @Query(value = 
        "SELECT g.* FROM goals g " +
        "WHERE g.status = 'IN_PROGRESS' " +
        "AND g.updated_at < :lastUpdate " +
        "ORDER BY g.id " +
        "LIMIT :batchSize",
        nativeQuery = true)
    List<Goal> findGoalsForUpdate(
        @Param("lastUpdate") LocalDateTime lastUpdate,
        @Param("batchSize") int batchSize);
    
    @Modifying
    @Query(value = 
        "UPDATE goals " +
        "SET current_value = current_value + :increment " +
        "WHERE id IN :ids",
        nativeQuery = true)
    int updateGoalValues(
        @Param("ids") List<UUID> ids,
        @Param("increment") BigDecimal increment);
}
```

### 3.3 캐시 적용
```java
@Repository
public interface CacheableRepository {
    
    @Cacheable(value = "userProfiles", key = "#userId")
    Optional<UserProfile> findProfileByUserId(UUID userId);
    
    @Cacheable(value = "goalCategories")
    List<GoalCategory> findAllCategories();
    
    @CacheEvict(value = "userProfiles", key = "#userId")
    @Modifying
    @Query("UPDATE UserProfile up SET up.bio = :bio " +
           "WHERE up.userId = :userId")
    int updateUserBio(
        @Param("userId") UUID userId, 
        @Param("bio") String bio);
}
```

## 4. 테스트 전략

### 4.1 리포지토리 테스트
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class UserRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void findByEmail_WhenUserExists_ShouldReturnUser() {
        // given
        String email = "test@example.com";
        User user = User.builder()
            .email(email)
            .passwordHash("hash")
            .nickname("tester")
            .build();
        userRepository.save(user);
        
        // when
        Optional<User> found = userRepository.findByEmail(email);
        
        // then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(email);
    }
    
    @Test
    void findByIdWithProfile_ShouldFetchProfileEagerly() {
        // given
        User user = User.builder()
            .email("test@example.com")
            .passwordHash("hash")
            .nickname("tester")
            .build();
        UserProfile profile = UserProfile.builder()
            .user(user)
            .build();
        user.setProfile(profile);
        userRepository.save(user);
        
        // when
        Optional<User> found = userRepository
            .findByIdWithProfile(user.getId());
        
        // then
        assertThat(found).isPresent();
        assertThat(found.get().getProfile())
            .isNotNull()
            .extracting(UserProfile::getUserId)
            .isEqualTo(user.getId());
    }
}
```

### 4.2 성능 테스트
```java
@SpringBootTest
class RepositoryPerformanceTest {
    
    @Autowired
    private GoalRepository goalRepository;
    
    @Test
    void measureQueryPerformance() {
        // given
        StopWatch stopWatch = new StopWatch();
        
        // when
        stopWatch.start("findAllByUserId");
        List<Goal> goals = goalRepository
            .findAllByUserId(UUID.randomUUID());
        stopWatch.stop();
        
        // then
        assertThat(stopWatch.getTotalTimeMillis())
            .isLessThan(1000);
    }
    
    @Test
    void verifyNPlusOnePrevention() {
        // given
        EntityManager em = getEntityManager();
        
        // when
        List<Goal> goals = goalRepository
            .findAllByUserIdWithDetails(UUID.randomUUID());
        
        // then
        assertThat(getPreviousQueryCount())
            .isEqualTo(1);
    }
}

```