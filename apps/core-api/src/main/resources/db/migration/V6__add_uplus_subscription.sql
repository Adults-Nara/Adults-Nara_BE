-- V6__add_uplus_subscription.sql
-- U+ 가입 정보 및 포인트 자동 할인 이력 테이블

-- =============================================
-- 1. U+ 가입 정보
-- =============================================
CREATE TABLE uplus_subscription
(
    uplus_subscription_id BIGINT       NOT NULL,
    user_id               BIGINT       NOT NULL,
    phone_number          VARCHAR(20)  NOT NULL,
    plan                  VARCHAR(50)  NOT NULL,
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT pk_uplus_subscription PRIMARY KEY (uplus_subscription_id),
    CONSTRAINT fk_uplus_subscription_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT uk_uplus_subscription_user  UNIQUE (user_id),
    CONSTRAINT uk_uplus_subscription_phone UNIQUE (phone_number)
);

CREATE INDEX idx_uplus_subscription_active ON uplus_subscription (active);

-- =============================================
-- 2. U+ 포인트 자동 할인 이력
--    스케줄러가 매월 1일 생성
-- =============================================
CREATE TABLE uplus_bill_discount
(
    uplus_bill_discount_id BIGINT      NOT NULL,
    user_id                BIGINT      NOT NULL,
    billing_year_month     VARCHAR(7)  NOT NULL,  -- YYYY-MM
    plan                   VARCHAR(50) NOT NULL,
    discount_amount        INT         NOT NULL DEFAULT 0,
    created_at             TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP   NOT NULL DEFAULT now(),

    CONSTRAINT pk_uplus_bill_discount PRIMARY KEY (uplus_bill_discount_id),
    CONSTRAINT fk_uplus_bill_discount_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT uk_uplus_bill_discount_user_month UNIQUE (user_id, billing_year_month)
);

CREATE INDEX idx_uplus_bill_discount_user ON uplus_bill_discount (user_id);