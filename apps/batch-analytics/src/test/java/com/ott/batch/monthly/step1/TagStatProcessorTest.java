package com.ott.batch.monthly.step1;

import com.ott.batch.monthly.dto.TagStatDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class TagStatProcessorTest {

    private final TagStatProcessor processor = new TagStatProcessor();

    @Test
    @DisplayName("정상적인 TagStatDto는 그대로 pass-through 한다")
    void processValidTagStatDto() throws Exception {
        // Given
        TagStatDto dto = TagStatDto.builder()
                .userId(1L)
                .tagId(10L)
                .statsDate(LocalDate.of(2025, 2, 1))
                .totalViewTime(7200L)
                .viewCount(5)
                .completedCount(3)
                .build();

        // When
        TagStatDto result = processor.process(dto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getTagId()).isEqualTo(10L);
        assertThat(result.getStatsDate()).isEqualTo(LocalDate.of(2025, 2, 1));
        assertThat(result.getTotalViewTime()).isEqualTo(7200L);
        assertThat(result.getViewCount()).isEqualTo(5);
        assertThat(result.getCompletedCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("viewCount가 0 이하인 경우 null을 반환하여 skip한다")
    void processInvalidViewCount() throws Exception {
        // Given
        TagStatDto dto = TagStatDto.builder()
                .userId(1L)
                .tagId(10L)
                .statsDate(LocalDate.of(2025, 2, 1))
                .totalViewTime(0L)
                .viewCount(0)  // invalid
                .completedCount(0)
                .build();

        // When
        TagStatDto result = processor.process(dto);

        // Then
        assertThat(result).isNull();  // skip
    }

    @Test
    @DisplayName("viewCount가 null인 경우 null을 반환하여 skip한다")
    void processNullViewCount() throws Exception {
        // Given
        TagStatDto dto = TagStatDto.builder()
                .userId(1L)
                .tagId(10L)
                .statsDate(LocalDate.of(2025, 2, 1))
                .totalViewTime(1000L)
                .viewCount(null)  // invalid
                .completedCount(0)
                .build();

        // When
        TagStatDto result = processor.process(dto);

        // Then
        assertThat(result).isNull();  // skip
    }
}