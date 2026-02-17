package com.ott.core.modules.video.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class VideoValidator {
    public static void validateTitleLength(String title) {
        if (title == null || title.length() < 2 || title.length() > 100) {
            throw new BusinessException(ErrorCode.VIDEO_INVALID_TITLE);
        }
    }

    public static void validateDescription(String description) {
        if (description != null && description.length() > 4000) {
            throw new BusinessException(ErrorCode.VIDEO_INVALID_DESCRIPTION);
        }
    }
}
