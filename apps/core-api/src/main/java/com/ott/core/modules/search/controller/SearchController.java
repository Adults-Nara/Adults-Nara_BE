package com.ott.core.modules.search.controller;

import com.ott.common.persistence.enums.VideoType;
import com.ott.common.response.ApiResponse;
import com.ott.core.modules.search.dto.VideoSearchResponse;
import com.ott.core.modules.search.service.VideoSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Tag(name = "검색 API", description = "Elasticsearch 기반 영상 검색 및 실시간 자동완성 기능")
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final VideoSearchService videoSearchService;

    @Operation(
            summary = "메인 영상 검색",
            description = "키워드, 영상 타입, 태그를 조합하여 영상을 검색합니다. (주의: 깊은 페이징 방지를 위해 최대 10,000번째 결과까지만 조회 가능합니다.)"
    )
    @GetMapping
    public ApiResponse<Page<VideoSearchResponse>> searchVideos(
            @Parameter(description = "검색어 (제목, 설명, 태그에 포함된 단어)", example = "아이언맨")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "영상 타입 필터링 (예: LONG, SHORT)", example = "LONG")
            @RequestParam(required = false) VideoType videoType,

            @Parameter(description = "특정 태그로 필터링", example = "액션")
            @RequestParam(required = false) String tag,

            @Parameter(description = "요청할 페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지당 데이터 개수", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        int requestedWindow = (page * size) + size;
        if (requestedWindow > 10000) {
            throw new IllegalArgumentException("요청하신 페이지가 너무 깊습니다. 최대 10,000번째 결과까지만 조회 가능합니다.");
        }
        Pageable pageable = PageRequest.of(page, size);

        Page<VideoSearchResponse> result = videoSearchService.searchVideos(keyword, videoType, tag, pageable);
        return ApiResponse.success(result);
    }

    @Operation(
            summary = "검색어 자동완성 (실시간 추천)",
            description = "사용자가 검색창에 타이핑을 할 때 실시간으로 호출되어 추천 검색어(자동완성 목록)를 반환합니다."
    )
    @GetMapping("/autocomplete")
    public ApiResponse<List<String>> autocomplete(
            @Parameter(description = "현재까지 입력된 부분 검색어", example = "아이")
            @RequestParam String keyword) {
        List<String> suggestions = videoSearchService.autocomplete(keyword);
        return ApiResponse.success(suggestions);
    }
}