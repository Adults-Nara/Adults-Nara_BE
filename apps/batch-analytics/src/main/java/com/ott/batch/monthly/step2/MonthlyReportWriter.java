package com.ott.batch.monthly.step2;

import com.ott.batch.monthly.dto.MonthlyReportDto;
import com.ott.common.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

/**
 * Step 2: 월간 리포트 Writer (Batch Update + Buffering)
 *
 * 🟢 IMPROVEMENT: N+1 쓰기 → Batch Update로 성능 개선
 * 청크 단위로 모아서 배치 처리
 */
@Slf4j
@Component
public class MonthlyReportWriter implements ItemStreamWriter<MonthlyReportDto>, StepExecutionListener {

    private final JdbcTemplate jdbcTemplate;
    private final List<MonthlyReportDto> buffer = new ArrayList<>();

    public MonthlyReportWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void write(Chunk<? extends MonthlyReportDto> chunk) {
        buffer.addAll(chunk.getItems());
        log.debug("[MonthlyReportWriter] 청크 {}건 버퍼에 추가 (현재 버퍼: {}건)", chunk.size(), buffer.size());
    }

    @Override
    public void open(ExecutionContext executionContext) {
        buffer.clear();
        log.debug("[MonthlyReportWriter] Writer 초기화");
    }

    @Override
    public void update(ExecutionContext executionContext) {
        // 중간 체크포인트에서 flush
        if (!buffer.isEmpty()) {
            flushBuffer();
        }
    }

    @Override
    public void close() {
        // Step 종료 시 남은 데이터 flush
        if (!buffer.isEmpty()) {
            flushBuffer();
        }
        log.debug("[MonthlyReportWriter] Writer 종료");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        // Step 완료 후 최종 flush (보험)
        if (!buffer.isEmpty()) {
            log.warn("[MonthlyReportWriter] afterStep에서 남은 데이터 발견. flush 실행");
            flushBuffer();
        }
        return ExitStatus.COMPLETED;
    }

    private void flushBuffer() {
        if (buffer.isEmpty()) {
            return;
        }

        String sql = """
            INSERT INTO monthly_watch_report (
                monthly_watch_report_id, user_id, report_year_month, total_watch_seconds, total_watch_count,
                completed_count, completion_rate, dawn_count, morning_count, afternoon_count, evening_count,
                night_count, peak_time_slot, longest_session_seconds, most_watched_tag_name, diversity_score, created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            ON CONFLICT (user_id, report_year_month) DO UPDATE SET
                total_watch_seconds      = EXCLUDED.total_watch_seconds,
                total_watch_count        = EXCLUDED.total_watch_count,
                completed_count          = EXCLUDED.completed_count,
                completion_rate          = EXCLUDED.completion_rate,
                dawn_count               = EXCLUDED.dawn_count,
                morning_count            = EXCLUDED.morning_count,
                afternoon_count          = EXCLUDED.afternoon_count,
                evening_count            = EXCLUDED.evening_count,
                night_count              = EXCLUDED.night_count,
                peak_time_slot           = EXCLUDED.peak_time_slot,
                longest_session_seconds  = EXCLUDED.longest_session_seconds,
                most_watched_tag_name    = EXCLUDED.most_watched_tag_name,
                diversity_score          = EXCLUDED.diversity_score,
                updated_at               = NOW()
        """;

        jdbcTemplate.batchUpdate(sql, buffer, buffer.size(), (PreparedStatement ps, MonthlyReportDto dto) -> {
            ps.setLong(1, IdGenerator.generate());
            ps.setLong(2, dto.getUserId());
            ps.setString(3, dto.getReportYearMonth());
            ps.setLong(4, dto.getTotalWatchSeconds() != null ? dto.getTotalWatchSeconds() : 0L);
            ps.setInt(5, dto.getTotalWatchCount() != null ? dto.getTotalWatchCount() : 0);
            ps.setInt(6, dto.getCompletedCount() != null ? dto.getCompletedCount() : 0);
            ps.setDouble(7, dto.getCompletionRate() != null ? dto.getCompletionRate() : 0.0);
            ps.setInt(8, dto.getDawnCount() != null ? dto.getDawnCount() : 0);
            ps.setInt(9, dto.getMorningCount() != null ? dto.getMorningCount() : 0);
            ps.setInt(10, dto.getAfternoonCount() != null ? dto.getAfternoonCount() : 0);
            ps.setInt(11, dto.getEveningCount() != null ? dto.getEveningCount() : 0);
            ps.setInt(12, dto.getNightCount() != null ? dto.getNightCount() : 0);
            ps.setString(13, dto.getPeakTimeSlot());
            ps.setInt(14, dto.getLongestSessionSeconds() != null ? dto.getLongestSessionSeconds() : 0);
            ps.setString(15, dto.getMostWatchedTagName());
            ps.setInt(16, dto.getDiversityScore() != null ? dto.getDiversityScore() : 0);
        });

        log.info("[MonthlyReportWriter] {}건 batch upsert 완료", buffer.size());
        buffer.clear();
    }
}