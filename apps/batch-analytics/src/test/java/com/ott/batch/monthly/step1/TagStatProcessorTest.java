package com.ott.batch.monthly.step1;

import com.ott.batch.monthly.dto.TagStatDto;
import com.ott.batch.monthly.dto.UserTagWatchRaw;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class TagStatProcessorTest {

    @Test
    @DisplayName("UserTagWatchRaw를 TagStatDto로 정상 변환한다")
    void processUserTagWatchRaw() throws Exception {
        // Given
        TagStatProcessor processor = new TagStatProcessor("2025-02");
        UserTagWatchRaw raw = new UserTagWatchRaw(
                1L,          // userId
                10L,         // tagId
                "드라마",     // tagName
                7200,        // totalViewTime
                5            // viewCount
        );

        // When
        TagStatDto result = processor.process(raw);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getTagId()).isEqualTo(10L);
        assertThat(result.getTagName()).isEqualTo("드라마");
        assertThat(result.getStatsDate()).isEqualTo(LocalDate.of(2025, 2, 1));
        assertThat(result.getTotalViewTime()).isEqualTo(7200);
        assertThat(result.getViewCount()).isEqualTo(5);
    }
}