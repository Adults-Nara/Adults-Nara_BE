package com.ott.core.modules.ads.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AdService {
    private final VideoMetadataRepository videoMetadataRepository;

    public VideoMetadata getAd() {
        return videoMetadataRepository.findRandomAd()
                .orElseThrow(() -> new BusinessException(ErrorCode.AD_NOT_FOUND));
    }
}
