package com.ott.core.modules.video.controller.request;

import com.ott.common.persistence.enums.VideoType;
import com.ott.common.persistence.enums.Visibility;

import java.util.List;

public record UploadRequest(
        String title,
        String description,
        VideoType videoType,
        String otherVideoUrl,
        List<Long> tagIds,
        Visibility visibility
) {

}
