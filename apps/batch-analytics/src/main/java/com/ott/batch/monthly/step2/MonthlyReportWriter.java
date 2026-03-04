package com.ott.batch.monthly.step2;

import com.ott.batch.monthly.dto.MonthlyReportDto;

import com.ott.batch.repository.MonthlyWatchReportRepository;
import com.ott.common.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Step2 Writer: MonthlyReportDto → monthly_watch_report upsert.
 *
 * StepExecutionListener를 함께 구현하여
 * afterStep() 시점에 Processor 버퍼에 남아있는 마지막 userId 집계를 flush한다.
 *
 * ※ MonthlyReportProcessor와 동일한 Bean scope(@StepScope)이어야 한다.
 */
@Slf4j
@Component("monthlyReportItemWriter")
@RequiredArgsConstructor
public class MonthlyReportWriter implements ItemWriter<MonthlyReportDto>, StepExecutionListener {

    private final MonthlyWatchReportRepository monthlyWatchReportRepository;
    private final MonthlyReportProcessor monthlyReportProcessor;

    @Override
    public void write(Chunk<? extends MonthlyReportDto> chunk) {
        List<? extends MonthlyReportDto> items = chunk.getItems();
        upsertAll(items);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        // Processor 버퍼에 남아있는 마지막 userId 처리
        MonthlyReportDto last = monthlyReportProcessor.flush();
        if (last != null) {
            log.info("[MonthlyReportWriter] 마지막 userId={} flush 처리", last.getUserId());
            upsertAll(List.of(last));
        }
        return stepExecution.getExitStatus();
    }

    private void upsertAll(List<? extends MonthlyReportDto> items) {
        for (MonthlyReportDto dto : items) {
            monthlyWatchReportRepository.upsertMonthlyReport(
                    IdGenerator.generate(),
                    dto.getUserId(),
                    dto.getReportYearMonth(),
                    dto.getTotalWatchSeconds(),
                    dto.getTotalWatchCount(),
                    dto.getCompletedCount(),
                    dto.getCompletionRate(),
                    dto.getDawnCount(),
                    dto.getMorningCount(),
                    dto.getAfternoonCount(),
                    dto.getEveningCount(),
                    dto.getNightCount(),
                    dto.getPeakTimeSlot(),
                    dto.getLongestSessionSeconds(),
                    dto.getMostWatchedTagName(),
                    dto.getDiversityScore()
            );
        }
        if (!items.isEmpty()) {
            log.info("[MonthlyReportWriter] {}건 upsert 완료", items.size());
        }
    }
}