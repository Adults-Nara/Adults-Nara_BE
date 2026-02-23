package com.ott.core.modules.preference.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.preference.dto.TagScoreDto;
import com.ott.core.modules.preference.service.UserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
@Tag(name = "유저 선호 태그 API", description = "사용자 취향 태그 조회")
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    /**
     * 내 선호 태그 Top N 조회
     */
    @Operation(
            summary = "내 선호 태그 조회",
            description = "현재 로그인한 사용자의 상위 N개 선호 태그와 점수를 반환합니다."
    )
    @GetMapping("/me")
    public ApiResponse<List<TagScoreDto>> getMyPreferences(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Long userId = Long.parseLong(authentication.getName());
        List<TagScoreDto> preferences = userPreferenceService.getTopPreferences(userId, limit);
        return ApiResponse.success(preferences);
    }

    /**
     * 특정 유저의 선호 태그 조회 (관리자 전용)
     */
    @Operation(
            summary = "특정 유저 선호 태그 조회 (관리자)",
            description = "관리자만 접근 가능합니다. 특정 사용자의 상위 N개 선호 태그와 점수를 반환합니다."
    )
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<TagScoreDto>> getUserPreferences(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<TagScoreDto> preferences = userPreferenceService.getTopPreferences(userId, limit);
        return ApiResponse.success(preferences);
    }
}