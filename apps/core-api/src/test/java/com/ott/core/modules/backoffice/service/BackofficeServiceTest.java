package com.ott.core.modules.backoffice.service;

import com.ott.core.modules.backoffice.dto.UploaderContentResponse;
import com.ott.core.modules.backoffice.repository.VideoMetadataQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackofficeServiceTest {

    @Mock VideoMetadataQueryRepository videoMetadataQueryRepository;
    @InjectMocks BackofficeService backofficeService;

    @Test
    void getUploaderContents_정상호출() {
        Long userId = 1L;
        String keyword = "test";
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageImpl<UploaderContentResponse> expected = new PageImpl<>(List.of());

        when(videoMetadataQueryRepository.findUploaderContents(userId, keyword, pageable)).thenReturn(expected);

        Page<UploaderContentResponse> result = backofficeService.getUploaderContents(userId, keyword, pageable);

        assertThat(result).isSameAs(expected);
        verify(videoMetadataQueryRepository).findUploaderContents(userId, keyword, pageable);
    }
}