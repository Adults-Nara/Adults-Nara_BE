-- 기존 monthly_watch_report 테이블 삭제
DROP TABLE IF EXISTS monthly_watch_report CASCADE;

-- 월간 태그별 통계 테이블 재생성
CREATE TABLE monthly_watch_report (
                                      monthly_watch_report_id BIGINT PRIMARY KEY,
                                      user_id BIGINT NOT NULL,
                                      stats_year INT NOT NULL,
                                      stats_month INT NOT NULL,
                                      tag_id BIGINT NOT NULL,
                                      tag_name VARCHAR(100) NOT NULL,
                                      total_watch_seconds BIGINT NOT NULL DEFAULT 0,
                                      watch_count INT NOT NULL DEFAULT 0,

                                      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                      CONSTRAINT monthly_watch_report_unique UNIQUE (user_id, stats_year, stats_month, tag_id),
                                      CONSTRAINT fk_monthly_watch_report_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                                      CONSTRAINT fk_monthly_watch_report_tag FOREIGN KEY (tag_id) REFERENCES tag(tag_id) ON DELETE CASCADE
);

CREATE INDEX idx_monthly_watch_report_user_year_month ON monthly_watch_report(user_id, stats_year, stats_month);
CREATE INDEX idx_monthly_watch_report_year_month ON monthly_watch_report(stats_year, stats_month);
CREATE INDEX idx_monthly_watch_report_user_tag ON monthly_watch_report(user_id, tag_id);

COMMENT ON TABLE monthly_watch_report IS '월간 태그별 시청 통계';
COMMENT ON COLUMN monthly_watch_report.stats_year IS '통계 연도 (예: 2026)';
COMMENT ON COLUMN monthly_watch_report.stats_month IS '통계 월 (1-12)';
COMMENT ON COLUMN monthly_watch_report.total_watch_seconds IS '해당 태그 총 시청 시간(초)';
COMMENT ON COLUMN monthly_watch_report.watch_count IS '해당 태그 시청 횟수';