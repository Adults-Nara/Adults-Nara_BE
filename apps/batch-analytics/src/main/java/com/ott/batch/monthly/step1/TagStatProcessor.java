package com.ott.batch.monthly.step1;

import com.ott.batch.monthly.dto.TagStatDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Step 1: 태그별 통계 Processor
 * (현재는 pass-through만 수행)
 */
@Slf4j
@Component
public class TagStatProcessor implements ItemProcessor<TagStatDto, TagStatDto> {

    @Override
    public TagStatDto process(TagStatDto item) {
        // 필요시 검증 로직 추가
        if (item.getViewCount() == null || item.getViewCount() <= 0) {
            log.warn("[TagStatProcessor] 유효하지 않은 데이터 스킵: {}", item);
            return null;  // null 반환 시 해당 아이템 skip
        }

        return item;  // pass-through
    }
}