package com.ott.core.modules.ads.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.entity.VideoTag;
import com.ott.core.modules.tag.repository.VideoTagRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AdService {
    private final VideoMetadataRepository videoMetadataRepository;
    private final VideoTagRepository videoTagRepository;

    public VideoMetadata getRandomAd() {
        return videoMetadataRepository.findRandomAd()
                .orElseThrow(() -> new BusinessException(ErrorCode.AD_NOT_FOUND));
    }

    public VideoMetadata getRelatedAd(Long videoId) {
        VideoMetadata videoMetadata = videoMetadataRepository.findByVideoIdAndDeleted(videoId, false)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_METADATA_NOT_FOUND));

        List<VideoTag> videoTags = videoTagRepository.findWithTagByVideoMetadataIdIn(List.of(videoMetadata.getId()));
        if (videoTags.isEmpty()) {
            return getRandomAd();
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(videoTags.size());
        VideoTag videoTag = videoTags.get(randomIndex);

        long tagId = videoTag.getTag().getId();
        return videoMetadataRepository.findRelatedRandomAd(tagId)
                .orElseGet(this::getRandomAd);
    }
}
