-- V3: OAuth 로그인 지원을 위한 users 테이블 제약조건 수정
-- 기존 check_auth_by_role 제약조건은 VIEWER가 반드시 oauth_provider='KAKAO'이어야 했으나,
-- 일반 회원가입(비밀번호) VIEWER도 존재할 수 있으므로 제약조건을 완화합니다.

-- 기존 제약조건 삭제
ALTER TABLE users DROP CONSTRAINT IF EXISTS check_auth_by_role;

-- 새 제약조건 추가
-- VIEWER: OAuth 로그인(password_hash NULL) 또는 일반 회원가입(password_hash NOT NULL) 모두 가능
-- UPLOADER/ADMIN: 반드시 password_hash가 있어야 함
ALTER TABLE users ADD CONSTRAINT check_auth_by_role CHECK (
    (role = 'VIEWER') OR
    (role IN ('UPLOADER', 'ADMIN') AND password_hash IS NOT NULL)
    );

-- OAuth 인덱스 확인 (이미 V2에서 생성되었지만 혹시 없으면)
CREATE INDEX IF NOT EXISTS idx_users_oauth ON users(oauth_provider, oauth_id) WHERE deleted = FALSE;

COMMENT ON COLUMN users.oauth_provider IS 'OAuth 제공자 (KAKAO)';
COMMENT ON COLUMN users.oauth_id IS 'OAuth 제공자의 사용자 고유 ID';