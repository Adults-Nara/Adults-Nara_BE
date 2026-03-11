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

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyReportWriter implements ItemWriter<MonthlyReportDto> {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void write(Chunk<? extends MonthlyReportDto> chunk) {
        chunk.getItems().forEach(dto -> {
            MonthlyWatchReport entity = dto.toEntity();

            // Upsert 로직
            MonthlyWatchReport existing = entityManager
                    .createQuery("SELECT m FROM MonthlyWatchReport m WHERE m.userId = :userId AND m.reportYearMonth = :yearMonth", MonthlyWatchReport.class)
                    .setParameter("userId", entity.getUserId())
                    .setParameter("yearMonth", entity.getReportYearMonth())
                    .getResultStream()
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                existing.update(entity);
                entityManager.merge(existing);  // 추가!
            } else {
                entityManager.persist(entity);
            }
        });

        entityManager.flush();  // 추가!

        log.debug("[MonthlyReportWriter] {}건 처리 완료", chunk.size());
    }
}