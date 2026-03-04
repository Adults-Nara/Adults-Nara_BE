package com.ott.batch.monthly.step1;

import com.ott.batch.monthly.dto.TagStatDto;

import com.ott.batch.repository.TagStatsRepository;
import com.ott.common.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Step1 Writer: TagStatDto → tag_stats 테이블 upsert.
 *
 * 동일 (user_id, tag_id, stats_date) 가 이미 있으면 누적, 없으면 insert.
 * 배치 재실행 시에도 중복 적립되지 않도록 upsert 사용.
 */
@Slf4j
@Component("tagStatItemWriter")
@RequiredArgsConstructor
public class TagStatWriter implements ItemWriter<TagStatDto> {

    private final TagStatsRepository tagStatsRepository;

    @Override
    public void write(Chunk<? extends TagStatDto> chunk) {
        for (TagStatDto dto : chunk.getItems()) {
            tagStatsRepository.upsertTagStats(
                    IdGenerator.generate(),
                    dto.getTagId(),
                    dto.getUserId(),
                    dto.getStatsDate(),
                    dto.getTotalViewTime(),
                    dto.getViewCount()
            );
        }
        log.debug("[TagStatWriter] {}건 upsert 완료", chunk.size());
    }
}