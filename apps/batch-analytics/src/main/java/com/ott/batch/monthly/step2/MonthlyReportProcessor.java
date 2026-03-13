package com.ott.batch.monthly.step2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Step 2: 사용자별 월간 리포트 Processor
 *
 * N+1 문제 해결을 위해 단순히 userId만 전달하고,
 * 실제 집계 로직은 Writer에서 청크 단위로 일괄 처리
 */
@Slf4j
@Component
public class MonthlyReportProcessor implements ItemProcessor<Long, Long> {

    @Override
    public Long process(Long userId) {
        // Writer에서 청크 단위로 처리하므로 단순 전달
        return userId;
    }
}