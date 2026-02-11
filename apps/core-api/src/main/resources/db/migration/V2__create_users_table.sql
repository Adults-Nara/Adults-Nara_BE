-- ENUM 타입 생성
CREATE TYPE user_role AS ENUM ('VIEWER', 'UPLOADER', 'ADMIN');

CREATE TYPE ban_status AS ENUM (
    'ACTIVE',
    'DEACTIVATED',
    'SUSPENDED_7',
    'SUSPENDED_15',
    'SUSPENDED_30',
    'PERMANENTLY_BANNED',
    'DELETED'
    );

-- Users 테이블 생성
CREATE TABLE users (
                       user_id BIGINT PRIMARY KEY,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password_hash VARCHAR(255),
                       nickname VARCHAR(50) NOT NULL,
                       profile_image_url TEXT,
                       role user_role NOT NULL DEFAULT 'VIEWER',
                       oauth_provider VARCHAR(20),
                       oauth_id VARCHAR(255),
                       deleted BOOLEAN NOT NULL DEFAULT FALSE,

    -- 계정 상태
                       banned ban_status NOT NULL DEFAULT 'ACTIVE',
                       banned_until TIMESTAMPTZ,
                       ban_reason TEXT,
                       banned_at TIMESTAMPTZ,

    -- BaseEntity 컬럼
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- 제약조건
                       CONSTRAINT check_auth_by_role CHECK (
                           (role = 'VIEWER' AND oauth_provider = 'KAKAO' AND password_hash IS NULL) OR
                           (role IN ('UPLOADER', 'ADMIN') AND password_hash IS NOT NULL AND oauth_provider IS NULL)
                           ),

                       CONSTRAINT check_banned_status CHECK (
                           (banned IN ('ACTIVE', 'DEACTIVATED', 'PERMANENTLY_BANNED', 'DELETED') AND banned_until IS NULL) OR
                           (banned IN ('SUSPENDED_7', 'SUSPENDED_15', 'SUSPENDED_30') AND banned_until IS NOT NULL)
                           )
);

-- 인덱스 (deleted = FALSE 조건 포함)
CREATE INDEX idx_users_email ON users(email) WHERE deleted = FALSE;
CREATE INDEX idx_users_oauth ON users(oauth_provider, oauth_id) WHERE deleted = FALSE;
CREATE INDEX idx_users_role ON users(role) WHERE deleted = FALSE;
CREATE INDEX idx_users_banned ON users(banned, banned_until);
CREATE INDEX idx_users_deleted ON users(deleted);

-- 코멘트
COMMENT ON TABLE users IS '사용자 테이블';
COMMENT ON COLUMN users.user_id IS '사용자 ID (TSID)';
COMMENT ON COLUMN users.email IS '이메일 (고유)';
COMMENT ON COLUMN users.password_hash IS '비밀번호 해시 (OAuth 사용자는 NULL)';
COMMENT ON COLUMN users.nickname IS '닉네임';
COMMENT ON COLUMN users.profile_image_url IS '프로필 이미지 S3 URL';
COMMENT ON COLUMN users.role IS '역할: VIEWER(일반), UPLOADER(업로더), ADMIN(관리자)';
COMMENT ON COLUMN users.oauth_provider IS 'OAuth 제공자 (KAKAO만 지원)';
COMMENT ON COLUMN users.oauth_id IS 'OAuth 사용자 ID';
COMMENT ON COLUMN users.deleted IS '삭제 여부 (소프트 삭제)';
COMMENT ON COLUMN users.banned IS '계정 상태';
COMMENT ON COLUMN users.banned_until IS '정지 종료일';
COMMENT ON COLUMN users.ban_reason IS '정지/삭제 사유';
COMMENT ON COLUMN users.banned_at IS '정지/삭제 시작일';
COMMENT ON COLUMN users.created_at IS '생성일 (BaseEntity)';
COMMENT ON COLUMN users.updated_at IS '최종 수정일 (BaseEntity에서 자동 갱신)';