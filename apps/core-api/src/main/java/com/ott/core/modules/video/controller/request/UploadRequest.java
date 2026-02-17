package com.ott.core.modules.video.controller.request;

import com.ott.common.persistence.enums.VideoType;

public record UploadRequest(
        String title,
        String description,
        VideoType videoType,
        String otherVideoUrl
) {

}
