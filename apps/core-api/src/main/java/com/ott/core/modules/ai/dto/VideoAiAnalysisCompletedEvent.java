package com.ott.core.modules.ai.dto;

import java.util.List;

public record VideoAiAnalysisCompletedEvent(
        Long videoId,
        String status,
        List<String> aiTags,
        String summary,
        String subtitleUrl,
        float[] embedding,
        String error) {
}
