package com.ott.batch.monthly.step2;

import com.ott.batch.monthly.dto.MonthlyReportDto;
import com.ott.common.persistence.entity.MonthlyWatchReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyReportWriter implements ItemWriter<MonthlyReportDto> {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void write(Chunk<? extends MonthlyReportDto> chunk) {
        if (chunk.getItems().isEmpty()) {
            return;
        }

        // 1. 한 번에 기존 데이터 조회 (N+1 방지)
        List<Long> userIds = chunk.getItems().stream()
                .map(MonthlyReportDto::getUserId)
                .toList();
        
        String yearMonth = chunk.getItems().get(0).getReportYearMonth();

        List<MonthlyWatchReport> existingReports = entityManager
                .createQuery(
                    "SELECT m FROM MonthlyWatchReport m WHERE m.userId IN :userIds AND m.reportYearMonth = :yearMonth", 
                    MonthlyWatchReport.class
                )
                .setParameter("userIds", userIds)
                .setParameter("yearMonth", yearMonth)
                .getResultList();

        Map<Long, MonthlyWatchReport> existingMap = existingReports.stream()
                .collect(Collectors.toMap(MonthlyWatchReport::getUserId, report -> report));

        // 2. Upsert 처리
        for (MonthlyReportDto dto : chunk.getItems()) {
            MonthlyWatchReport existing = existingMap.get(dto.getUserId());
            MonthlyWatchReport entity = dto.toEntity();

            if (existing != null) {
                existing.update(entity);
                // merge() 불필요 - 이미 영속성 컨텍스트가 관리 중
            } else {
                entityManager.persist(entity);
            }
        }

        log.debug("[MonthlyReportWriter] {}건 처리 완료", chunk.size());
    }
}
