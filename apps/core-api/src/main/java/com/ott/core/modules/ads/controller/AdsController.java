package com.ott.core.modules.ads.controller;

import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.response.ApiResponse;
import com.ott.core.modules.ads.controller.response.AdResponse;
import com.ott.core.modules.ads.service.AdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "광고 API", description = "광고 API")
@RestController
@RequiredArgsConstructor
public class AdsController {
    private final AdService adService;

    @Operation(
            summary = "광고 조회 API",
            description = "videoId와 관련된 광고 영상 ID를 조회합니다."
    )
    @GetMapping("/api/v1/ads")
    public ApiResponse<AdResponse> getAd(@PathParam("videoId") Long videoId) {
        VideoMetadata result = adService.getRelatedAd(videoId);
        return ApiResponse.success(AdResponse.of(result));
    }
}
