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
                       banned ban_status NOT NULL DEFAULT 'ACTIVE',

    -- ✅ TIMESTAMPTZ 사용 (타임존 정보 포함)
                       banned_until TIMESTAMPTZ,
                       ban_reason TEXT,
                       banned_at TIMESTAMPTZ,
                       banned_by BIGINT,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                       CONSTRAINT check_auth_by_role CHECK (
                           (role = 'VIEWER' AND oauth_provider = 'KAKAO' AND password_hash IS NULL) OR
                           (role IN ('UPLOADER', 'ADMIN') AND password_hash IS NOT NULL AND oauth_provider IS NULL)
                           ),

                       CONSTRAINT check_banned_status CHECK (
                           (banned IN ('ACTIVE', 'DEACTIVATED', 'PERMANENTLY_BANNED', 'DELETED') AND banned_until IS NULL) OR
                           (banned IN ('SUSPENDED_7', 'SUSPENDED_15', 'SUSPENDED_30') AND banned_until IS NOT NULL)
                           )
);

-- 인덱스
CREATE INDEX idx_users_email ON users(email) WHERE banned != 'DELETED';
CREATE INDEX idx_users_oauth ON users(oauth_provider, oauth_id) WHERE banned != 'DELETED';
CREATE INDEX idx_users_role ON users(role) WHERE banned != 'DELETED';
CREATE INDEX idx_users_banned ON users(banned, banned_until);