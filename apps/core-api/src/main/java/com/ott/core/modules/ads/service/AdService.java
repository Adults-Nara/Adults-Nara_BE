package com.ott.core.modules.ads.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.entity.VideoTag;
import com.ott.core.modules.tag.repository.VideoTagRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

        Collections.shuffle(videoTags); // 태그를 무작위 순서로 시도

        for (VideoTag videoTag : videoTags) {
            long tagId = videoTag.getTag().getId();
            Optional<VideoMetadata> ad = videoMetadataRepository.findRelatedRandomAd(tagId);
            if (ad.isPresent()) {
                return ad.get(); // 첫 번째로 찾은 연관 광고 반환
            }
        }

        // 모든 태그에서 연관 광고를 찾지 못한 경우
        return getRandomAd();
    }
}
