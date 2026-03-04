package com.ott.core.modules.video.controller.response;

import com.ott.common.persistence.enums.Visibility;
import com.ott.core.modules.video.dto.VideoInfoResult;
import com.ott.core.modules.watch.dto.response.WatchHistoryResponse;

import java.time.OffsetDateTime;
import java.util.List;

public record VideoInfoResponse(
        String videoId,
        String title,
        String description,
        String thumbnailUrl,
        Visibility visibility,
        List<String> tagIds,
        OffsetDateTime createdAt,
        String otherVideoUrl,
        String userProfile,
        String userNickname,
        WatchHistoryResponse watchHistory
) {
    public static VideoInfoResponse of(VideoInfoResult result, WatchHistoryResponse watchHistory) {
        return new VideoInfoResponse(
                result.videoId(),
                result.title(),
                result.description(),
                result.thumbnailUrl(),
                result.visibility(),
                result.tagIds(),
                result.createdAt(),
                result.otherVideoUrl(),
                result.userProfile(),
                result.userNickname(),
                watchHistory
        );
    }
}
