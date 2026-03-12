package com.ott.batch.monthly.step1;

import com.ott.batch.monthly.dto.TagStatDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TagStatProcessorTest {

    private TagStatProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TagStatProcessor();
        
        // StepExecution Mock 및 beforeStep 호출
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("yearMonth", "2025-02")
                .toJobParameters();
        
        StepExecution stepExecution = mock(StepExecution.class);
        when(stepExecution.getJobParameters()).thenReturn(jobParameters);
        
        processor.beforeStep(stepExecution);  // CRITICAL: 추가!
    }

    @Test
    @DisplayName("정상적인 TagStatDto는 statsDate가 설정되어 pass-through 한다")
    void processValidTagStatDto() throws Exception {
        // Given
        TagStatDto dto = TagStatDto.builder()
                .userId(1L)
                .tagId(10L)
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
        assertThat(result.getStatsDate()).isEqualTo(LocalDate.of(2025, 2, 1));  // beforeStep에서 설정됨
        assertThat(result.getTotalViewTime()).isEqualTo(7200L);
        assertThat(result.getViewCount()).isEqualTo(5);
        assertThat(result.getCompletedCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("viewCount가 0 이하면 null을 반환하여 skip 한다")
    void processInvalidViewCount() throws Exception {
        // Given
        TagStatDto dto = TagStatDto.builder()
                .userId(1L)
                .tagId(10L)
                .totalViewTime(0L)
                .viewCount(0)
                .completedCount(0)
                .build();

        // When
        TagStatDto result = processor.process(dto);

        // Then
        assertThat(result).isNull();
    }
}
