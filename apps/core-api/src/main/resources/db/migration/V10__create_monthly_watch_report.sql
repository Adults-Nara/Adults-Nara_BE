-- V10: 월별 시청 리포트 테이블
-- 매월 1일 배치가 전월 통계를 집계하여 저장

CREATE TABLE monthly_watch_report (
                                      monthly_watch_report_id  BIGINT       NOT NULL,
                                      user_id                  BIGINT       NOT NULL,
                                      report_year_month        VARCHAR(7)   NOT NULL,  -- YYYY-MM (전월 기준, 예: "2025-02")

    -- 기본 지표
                                      total_watch_seconds      BIGINT       NOT NULL DEFAULT 0,   -- 총 시청 시간(초)
                                      total_watch_count        INT          NOT NULL DEFAULT 0,   -- 총 시청 횟수
                                      completed_count          INT          NOT NULL DEFAULT 0,   -- 완주한 영상 수
                                      completion_rate          NUMERIC(5,2) NOT NULL DEFAULT 0,   -- 완주율 (%)

    -- 시간대별 시청 패턴
                                      dawn_count               INT          NOT NULL DEFAULT 0,   -- 새벽 (00~05시)
                                      morning_count            INT          NOT NULL DEFAULT 0,   -- 아침 (06~11시)
                                      afternoon_count          INT          NOT NULL DEFAULT 0,   -- 오후 (12~17시)
                                      evening_count            INT          NOT NULL DEFAULT 0,   -- 저녁 (18~21시)
                                      night_count              INT          NOT NULL DEFAULT 0,   -- 야간 (22~23시)
                                      peak_time_slot           VARCHAR(20)  NOT NULL DEFAULT 'NONE', -- DAWN/MORNING/AFTERNOON/EVENING/NIGHT

    -- 기록
                                      longest_session_seconds  INT          NOT NULL DEFAULT 0,   -- 한 영상 최장 연속 시청(초)
                                      most_watched_tag_name    VARCHAR(100),                      -- 이달의 최애 태그

    -- 다양성 점수: 0~100점, 시청한 고유 태그 수 기반
                                      diversity_score          INT          NOT NULL DEFAULT 0,

                                      created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                                      updated_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

                                      CONSTRAINT pk_monthly_watch_report PRIMARY KEY (monthly_watch_report_id),
                                      CONSTRAINT fk_monthly_watch_report_user FOREIGN KEY (user_id) REFERENCES users(user_id),
                                      CONSTRAINT uk_monthly_watch_report_user_month UNIQUE (user_id, report_year_month)
);

CREATE INDEX idx_monthly_watch_report_user ON monthly_watch_report(user_id);
CREATE INDEX idx_monthly_watch_report_month ON monthly_watch_report(report_year_month);


ALTER TABLE tag_stats
    ADD CONSTRAINT uk_tag_stats_tag_user_date
        UNIQUE (tag_id, user_id, stats_date);


COMMENT ON TABLE monthly_watch_report IS '월별 개인 시청 리포트 (배치 집계)';
COMMENT ON COLUMN monthly_watch_report.report_year_month IS '집계 대상 연월 (YYYY-MM). 예: 3월 1일 배치 실행 시 2월 통계 → "2025-02"';
COMMENT ON COLUMN monthly_watch_report.peak_time_slot IS '가장 많이 시청한 시간대: DAWN/MORNING/AFTERNOON/EVENING/NIGHT';
COMMENT ON COLUMN monthly_watch_report.diversity_score IS '시청 다양성 점수 0~100. 고유 태그 수 기반으로 산출';
COMMENT ON COLUMN monthly_watch_report.longest_session_seconds IS '한 번에 가장 오래 시청한 영상의 시청 시간(초)';