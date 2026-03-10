package com.ott.core.modules.ads.controller.response;

import com.ott.common.persistence.entity.VideoMetadata;

public record AdResponse(
        String videoId
) {
    public static AdResponse of(VideoMetadata videoMetadata) {
        return new AdResponse(videoMetadata.getVideoId().toString());
    }
}
