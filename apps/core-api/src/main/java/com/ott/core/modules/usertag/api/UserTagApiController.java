package com.ott.core.modules.usertag.api;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.usertag.dto.OnboardingTagRequest;
import com.ott.core.modules.usertag.dto.TagWatchStatsResponse;
import com.ott.core.modules.usertag.dto.UpdateUserTagRequest;
import com.ott.core.modules.usertag.service.UserTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user-tag")
@Tag(name = "유저 태그 API", description = "사용자 관심 태그 설정 및 태그별 시청 통계 API")
public class UserTagApiController {

    private final UserTagService userTagService;

    @Operation(summary = "온보딩 태그 저장", description = "온보딩 시 사용자가 선택한 관심 태그를 저장합니다.")
    @PostMapping("/onboarding")
    public ApiResponse<?> saveOnboardingTags(@AuthenticationPrincipal String userId, @Valid @RequestBody OnboardingTagRequest request) {
        userTagService.saveOnboardingTags(Long.parseLong(userId), request.tagIds());
        return ApiResponse.success();
    }

    @Operation(summary = "관심 태그 수정", description = "사용자의 관심 태그 목록을 새로운 목록으로 교체합니다.")
    @PutMapping
    public ApiResponse<?> updateUserTags(@AuthenticationPrincipal String userId, @Valid @RequestBody UpdateUserTagRequest request) {
        userTagService.updateUserTags(Long.parseLong(userId), request.tagIds());
        return ApiResponse.success();
    }

    @Operation(summary = "태그별 시청 통계 조회", description = "사용자가 관심 설정한 태그별 시청 횟수 통계를 반환합니다.")
    @GetMapping("/tag-watch-stats")
    public ApiResponse<List<TagWatchStatsResponse>> getTagWatchStats(@AuthenticationPrincipal String userId) {
        List<TagWatchStatsResponse> response = userTagService.getTagWatchStats(Long.parseLong(userId));
        return ApiResponse.success(response);
    }
}
