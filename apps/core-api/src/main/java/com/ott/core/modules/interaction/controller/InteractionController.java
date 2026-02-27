package com.ott.core.modules.interaction.controller;

import com.ott.common.persistence.enums.InteractionType;
import com.ott.common.response.ApiResponse;
import com.ott.core.modules.interaction.dto.InteractionStatusResponseDto;
import com.ott.core.modules.interaction.service.InteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "상호작용 API", description = "좋아요/싫어요 기능")
@RestController
@RequestMapping("/api/v1/interactions") // v1 추가 (버전 관리)
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    // 좋아요
    @Operation(summary = "좋아요 누르기", description = "해당 영상에 좋아요를 표시합니다.")
    @PostMapping("/{videoId}/like")
    public ApiResponse<?> likeVideo(
            @PathVariable Long videoId,
            @AuthenticationPrincipal String userId) {
        interactionService.interact(Long.parseLong(userId), videoId, InteractionType.LIKE);
        return ApiResponse.success();
    }

    // 싫어요
    @Operation(summary = "싫어요 누르기", description = "해당 영상에 싫어요를 표시합니다.")
    @PostMapping("/{videoId}/dislike")
    public ApiResponse<?> dislikeVideo(
            @PathVariable Long videoId,
            @AuthenticationPrincipal String userId) {
        interactionService.interact(Long.parseLong(userId), videoId, InteractionType.DISLIKE);
        return ApiResponse.success();
    }

    // 슈퍼라이크 (왕따봉)
    @Operation(summary = "슈퍼라이크(왕따봉) 누르기", description = "해당 영상에 최고예요를 표시합니다.")
    @PostMapping("/{videoId}/superlike")
    public ApiResponse<?> superLikeVideo(
            @PathVariable Long videoId,
            @AuthenticationPrincipal String userId) {
        interactionService.interact(Long.parseLong(userId), videoId, InteractionType.SUPERLIKE);
        return ApiResponse.success();
    }


    @Operation(summary = "내 반응 조회", description = "내가 이 영상에 좋아요/싫어요를 했는지 확인합니다.")
    @GetMapping("/{videoId}/my-status")
    public ApiResponse<InteractionStatusResponseDto> getMyInteractionStatus(
            @PathVariable Long videoId,
            @AuthenticationPrincipal String userId) {

        InteractionType type = interactionService.getInteractionStatus(Long.parseLong(userId), videoId).orElse(null);

        InteractionStatusResponseDto responseDto = InteractionStatusResponseDto.from(type);

        return ApiResponse.success(responseDto);
    }
}