package com.ott.core.modules.search.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.docs.SearchAdminApiDocs;
import com.ott.core.modules.search.service.VideoSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search/admin")
@RequiredArgsConstructor
public class SearchAdminController implements SearchAdminApiDocs {

    private final VideoSyncService videoSyncService;
    @Override
    @PostMapping("/sync")
    public ApiResponse<String> syncData() {
        videoSyncService.syncAllVideosToElasticsearch();
        String message = "✅ 엘라스틱서치 데이터 동기화가 백그라운드에서 완료되었습니다. 콘솔 로그를 확인하세요!";
        return ApiResponse.success(message);
    }
}
