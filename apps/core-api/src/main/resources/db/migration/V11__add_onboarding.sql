
ALTER TABLE users
    ADD COLUMN onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN users.onboarding_completed IS '온보딩 완료 여부 (최초 로그인 시 false, 완료 후 true)';