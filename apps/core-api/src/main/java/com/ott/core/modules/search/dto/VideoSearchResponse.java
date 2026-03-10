package com.ott.core.modules.search.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ott.common.persistence.enums.VideoType;
import com.ott.core.modules.search.document.VideoDocument;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record VideoSearchResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long videoId,
        String thumbnailSrc,
        String title,
        String uploader,
        String uploaderProfileImageUrl,
        int progress,
        Integer duration,
        int views,
        String date,

        VideoType videoType
) {
    public static VideoSearchResponse of(VideoDocument document, String uploader, String profileUrl, int progress) {
        return VideoSearchResponse.builder()
                .videoId(document.getVideoId())
                .thumbnailSrc(document.getThumbnailUrl())
                .title(document.getTitle())
                .uploader(uploader)
                .uploaderProfileImageUrl(profileUrl)
                .progress(progress)
                .duration(document.getDuration() != null ? document.getDuration() : 0)
                .views(document.getViewCount())
                .date(document.getCreatedAt() != null ? document.getCreatedAt().toString() : null)
                .videoType(document.getVideoType())
                .build();
    }
}