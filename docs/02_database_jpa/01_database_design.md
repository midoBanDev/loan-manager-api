# 데이터베이스 설계 상세 가이드

## 1. 데이터베이스 설계 원칙

### 1.1 정규화 원칙
1. **제1정규형 (1NF)**
   - 모든 속성은 원자값을 가져야 함
   - 반복되는 그룹이 없어야 함
   - 복합 속성을 분해

2. **제2정규형 (2NF)**
   - 부분 함수적 종속성 제거
   - 모든 비주요 속성은 주요 키에 완전 함수적 종속

3. **제3정규형 (3NF)**
   - 이행적 종속성 제거
   - 비주요 속성 간의 종속성 제거

### 1.2 명명 규칙
```sql
-- 테이블명: 복수형, 소문자, 언더스코어
users, user_profiles, goal_progress

-- 컬럼명: 소문자, 언더스코어
first_name, last_name, created_at

-- 기본키: id
-- 외래키: 참조테이블명_id
user_id, goal_id

-- 인덱스: idx_테이블명_컬럼명
idx_users_email, idx_goals_user_id

-- 제약조건: 테이블명_컬럼명_type
users_email_unique, goals_title_not_null
```

## 2. 핵심 테이블 설계

### 2.1 사용자 관련 테이블
```sql
-- 사용자 기본 정보
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

-- 인덱스 생성
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_nickname ON users (nickname);

-- 사용자 프로필
CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY,
    bio TEXT,
    profile_image_url VARCHAR(255),
    birth_date DATE,
    gender VARCHAR(10),
    phone_number VARCHAR(20),
    address_main VARCHAR(255),
    address_detail VARCHAR(255),
    interests TEXT[],
    notification_settings JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT user_profiles_user_id_fk 
        FOREIGN KEY (user_id) 
        REFERENCES users (id) 
        ON DELETE CASCADE,
    CONSTRAINT user_profiles_gender_check 
        CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'))
);
```

**테이블 설명**:

1. **users 테이블**
   - `id`: UUID 타입의 기본키 (자동 생성)
   - `email`: 사용자 이메일 (유니크 제약조건)
   - `password_hash`: 암호화된 비밀번호
   - `status`: 사용자 상태 관리 (ACTIVE, INACTIVE, SUSPENDED)
   - `created_at`, `updated_at`: 생성/수정 시간 자동 기록

2. **user_profiles 테이블**
   - `user_id`: users 테이블 참조 (CASCADE 삭제)
   - `interests`: 배열 타입으로 관심사 저장
   - `notification_settings`: JSONB 타입으로 알림 설정 저장
   - 선택적 프로필 정보 (bio, profile_image_url 등)

### 2.2 목표 관련 테이블
```sql
-- 목표 카테고리
CREATE TABLE goal_categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description TEXT,
    parent_id INTEGER,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT goal_categories_name_unique UNIQUE (name),
    CONSTRAINT goal_categories_parent_id_fk 
        FOREIGN KEY (parent_id) 
        REFERENCES goal_categories (id)
);

-- 목표
CREATE TABLE goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    category_id INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    target_value DECIMAL,
    current_value DECIMAL DEFAULT 0,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT goals_user_id_fk 
        FOREIGN KEY (user_id) 
        REFERENCES users (id),
    CONSTRAINT goals_category_id_fk 
        FOREIGN KEY (category_id) 
        REFERENCES goal_categories (id),
    CONSTRAINT goals_date_check 
        CHECK (end_date >= start_date),
    CONSTRAINT goals_status_check 
        CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
    CONSTRAINT goals_visibility_check 
        CHECK (visibility IN ('PUBLIC', 'FRIENDS', 'PRIVATE'))
);

-- 인덱스 생성
CREATE INDEX idx_goals_user_id ON goals (user_id);
CREATE INDEX idx_goals_category_id ON goals (category_id);
CREATE INDEX idx_goals_status ON goals (status);

-- 목표 진행상황
CREATE TABLE goal_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id UUID NOT NULL,
    progress_date DATE NOT NULL,
    value DECIMAL NOT NULL,
    note TEXT,
    evidence_urls TEXT[],
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT goal_progress_goal_id_fk 
        FOREIGN KEY (goal_id) 
        REFERENCES goals (id) 
        ON DELETE CASCADE,
    CONSTRAINT goal_progress_unique_date 
        UNIQUE (goal_id, progress_date)
);

-- 인덱스 생성
CREATE INDEX idx_goal_progress_goal_id ON goal_progress (goal_id);
CREATE INDEX idx_goal_progress_date ON goal_progress (progress_date);
```

**테이블 설명**:

1. **goal_categories 테이블**
   - `id`: 자동 증가하는 정수형 기본키
   - `parent_id`: 자기 참조로 계층 구조 구현
   - `display_order`: 화면 표시 순서 관리

2. **goals 테이블**
   - `id`: UUID 타입의 기본키
   - `target_value`, `current_value`: 목표치와 현재 진행도
   - `status`: 목표 상태 관리
   - `visibility`: 공개 범위 설정

3. **goal_progress 테이블**
   - `goal_id`: goals 테이블 참조 (CASCADE 삭제)
   - `evidence_urls`: 증빙 자료 URL 배열
   - 날짜별 유니크 제약조건으로 중복 기록 방지

### 2.3 커뮤니티 관련 테이블
```sql
-- 그룹
CREATE TABLE groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    max_members INTEGER,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT groups_created_by_fk 
        FOREIGN KEY (created_by) 
        REFERENCES users (id),
    CONSTRAINT groups_visibility_check 
        CHECK (visibility IN ('PUBLIC', 'PRIVATE', 'SECRET')),
    CONSTRAINT groups_max_members_check 
        CHECK (max_members > 0)
);

-- 그룹 멤버십
CREATE TABLE group_members (
    group_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (group_id, user_id),
    
    CONSTRAINT group_members_group_id_fk 
        FOREIGN KEY (group_id) 
        REFERENCES groups (id) 
        ON DELETE CASCADE,
    CONSTRAINT group_members_user_id_fk 
        FOREIGN KEY (user_id) 
        REFERENCES users (id),
    CONSTRAINT group_members_role_check 
        CHECK (role IN ('OWNER', 'ADMIN', 'MODERATOR', 'MEMBER'))
);

-- 인덱스 생성
CREATE INDEX idx_group_members_user_id ON group_members (user_id);
```

**테이블 설명**:

1. **groups 테이블**
   - `max_members`: 최대 멤버 수 제한
   - `visibility`: 그룹 공개 범위
   - `created_by`: 그룹 생성자 참조

2. **group_members 테이블**
   - 복합 기본키 (group_id, user_id)
   - `role`: 그룹 내 역할 관리
   - CASCADE 삭제로 그룹 삭제 시 멤버십도 함께 삭제

## 3. 성능 최적화

### 3.1 인덱스 전략
```sql
-- 자주 조회되는 컬럼에 대한 인덱스
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_nickname ON users (nickname);
CREATE INDEX idx_goals_user_id ON goals (user_id);
CREATE INDEX idx_goals_status ON goals (status);

-- 복합 인덱스
CREATE INDEX idx_goals_user_status ON goals (user_id, status);
CREATE INDEX idx_progress_goal_date ON goal_progress (goal_id, progress_date);

-- 부분 인덱스
CREATE INDEX idx_active_users ON users (email) 
WHERE status = 'ACTIVE';
```

### 3.2 파티셔닝 전략
```sql
-- 날짜별 파티셔닝 예시
CREATE TABLE goal_progress_partitioned (
    id UUID NOT NULL,
    goal_id UUID NOT NULL,
    progress_date DATE NOT NULL,
    value DECIMAL NOT NULL,
    note TEXT,
    created_at TIMESTAMP NOT NULL
) PARTITION BY RANGE (progress_date);

-- 월별 파티션 생성
CREATE TABLE goal_progress_y2024m01 
    PARTITION OF goal_progress_partitioned
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE goal_progress_y2024m02 
    PARTITION OF goal_progress_partitioned
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');
```

## 4. 데이터 마이그레이션

### 4.1 마이그레이션 스크립트
```sql
-- 버전 관리 테이블
CREATE TABLE schema_version (
    version INTEGER PRIMARY KEY,
    description TEXT NOT NULL,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 마이그레이션 예시
BEGIN;

-- 버전 기록
INSERT INTO schema_version (version, description)
VALUES (1, 'Add user preferences');

-- 스키마 변경
ALTER TABLE user_profiles
ADD COLUMN preferences JSONB;

COMMIT;
```

### 4.2 롤백 스크립트
```sql
-- 롤백 예시
BEGIN;

-- 변경 취소
ALTER TABLE user_profiles
DROP COLUMN preferences;

-- 버전 기록 삭제
DELETE FROM schema_version
WHERE version = 1;

COMMIT;
```

## 5. 모니터링 및 유지보수

### 5.1 성능 모니터링 뷰
```sql
-- 테이블 크기 모니터링
CREATE VIEW vw_table_sizes AS
SELECT 
    schemaname,
    relname as table_name,
    pg_size_pretty(pg_total_relation_size(relid)) as total_size,
    pg_size_pretty(pg_relation_size(relid)) as table_size,
    pg_size_pretty(pg_total_relation_size(relid) - pg_relation_size(relid)) 
        as index_size
FROM pg_catalog.pg_statio_user_tables
ORDER BY pg_total_relation_size(relid) DESC;

-- 인덱스 사용 현황
CREATE VIEW vw_index_usage AS
SELECT 
    schemaname,
    relname as table_name,
    indexrelname as index_name,
    idx_scan as number_of_scans,
    idx_tup_read as tuples_read,
    idx_tup_fetch as tuples_fetched
FROM pg_catalog.pg_statio_user_indexes
ORDER BY idx_scan DESC;
```

### 5.2 유지보수 작업
```sql
-- 통계 정보 업데이트
ANALYZE users;
ANALYZE goals;
ANALYZE goal_progress;

-- 테이블 재구성
VACUUM FULL users;
VACUUM FULL goals;
VACUUM FULL goal_progress;

-- 인덱스 재구성
REINDEX TABLE users;
REINDEX TABLE goals;
REINDEX TABLE goal_progress;

```