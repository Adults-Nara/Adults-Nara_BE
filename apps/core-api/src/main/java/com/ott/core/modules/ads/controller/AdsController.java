package com.ott.core.modules.ads.controller;

import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.response.ApiResponse;
import com.ott.core.modules.ads.controller.response.AdResponse;
import com.ott.core.modules.ads.service.AdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdsController {
    private final AdService adService;

    @GetMapping("/api/v1/ads")
    public ApiResponse<AdResponse> getAd() {
        VideoMetadata result = adService.getAd();
        return ApiResponse.success(AdResponse.of(result));
    }
}
