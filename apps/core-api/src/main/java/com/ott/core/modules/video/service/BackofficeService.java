package com.ott.core.modules.video.service;

import com.ott.core.modules.video.dto.backoffice.UploaderContentResponse;
import com.ott.core.modules.video.repository.VideoMetadataQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BackofficeService {

    private final VideoMetadataQueryRepository videoMetadataQueryRepository;

    public Page<UploaderContentResponse> getUploaderContents(Long userId, String keyword, Pageable pageable) {
        return videoMetadataQueryRepository.findUploaderContents(userId, keyword, pageable);
    }
}
