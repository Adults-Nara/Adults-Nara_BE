package com.ott.core.modules.search.controller;

import com.ott.common.persistence.enums.VideoType;
import com.ott.common.response.ApiResponse;
import com.ott.core.modules.search.dto.VideoSearchResponse;
import com.ott.core.modules.search.service.VideoSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final VideoSearchService videoSearchService;

    /**
     * 메인 검색 API
     * GET /api/v1/search?keyword=아이언맨&videoType=NORMAL&tag=액션&page=0&size=20
     */
    @GetMapping
    public ApiResponse<Page<VideoSearchResponse>> searchVideos(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) VideoType videoType,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int requestedWindow = (page * size) + size;
        if (requestedWindow > 10000) {
            throw new IllegalArgumentException("요청하신 페이지가 너무 깊습니다. 최대 10,000번째 결과까지만 조회 가능합니다.");
        }
        Pageable pageable = PageRequest.of(page, size);

        Page<VideoSearchResponse> result = videoSearchService.searchVideos(keyword, videoType, tag, pageable);
        return ApiResponse.success(result);
    }

    /**
     * 실시간 자동완성 API (타이핑 시 호출)
     * GET /api/v1/search/autocomplete?keyword=아이
     */
    @GetMapping("/autocomplete")
    public ApiResponse<List<String>> autocomplete(@RequestParam String keyword) {
        List<String> suggestions = videoSearchService.autocomplete(keyword);
        return ApiResponse.success(suggestions);
    }
}