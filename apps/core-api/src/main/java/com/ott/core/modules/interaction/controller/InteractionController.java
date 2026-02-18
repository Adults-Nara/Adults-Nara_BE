package com.ott.core.modules.interaction.controller;

import com.ott.common.persistence.enums.InteractionType;
import com.ott.core.modules.interaction.service.InteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "상호작용 API", description = "좋아요/싫어요 기능")
@RestController
@RequestMapping("/api/v1/interactions") // v1 추가 (버전 관리)
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    // 좋아요
    @Operation(summary = "좋아요 누르기", description = "해당 영상에 좋아요를 표시합니다.")
    @PostMapping("/video/{videoId}/like")
    public ResponseEntity<String> likeVideo(
            @PathVariable Long videoId,
            @RequestParam Long userId) {
        interactionService.interact(userId, videoId, InteractionType.LIKE);
        return ResponseEntity.ok("성공: 좋아요 반영됨");
    }

    // 싫어요
    @Operation(summary = "싫어요 누르기", description = "해당 영상에 싫어요를 표시합니다.")
    @PostMapping("/video/{videoId}/dislike")
    public ResponseEntity<String> dislikeVideo(
            @PathVariable Long videoId,
            @RequestParam Long userId) {
        interactionService.interact(userId, videoId, InteractionType.DISLIKE);
        return ResponseEntity.ok("성공: 싫어요 반영됨");
    }

    // 슈퍼라이크 (왕따봉)
    @Operation(summary = "슈퍼라이크(왕따봉) 누르기", description = "해당 영상에 최고예요를 표시합니다.")
    @PostMapping("/video/{videoId}/superlike")
    public ResponseEntity<String> superLikeVideo(
            @PathVariable Long videoId,
            @RequestParam Long userId) {
        interactionService.interact(userId, videoId, InteractionType.SUPERLIKE); // Enum 이름 확인 필요 (SUPERLIKE 인지 SUPER_LIKE 인지)
        return ResponseEntity.ok("성공: 슈퍼라이크 반영됨");
    }


    @Operation(summary = "내 반응 조회", description = "내가 이 영상에 좋아요/싫어요를 했는지 확인합니다. (없으면 null)")
    @GetMapping("/video/{videoId}/my-status")
    public ResponseEntity<InteractionType> getMyInteractionStatus(
            @PathVariable Long videoId,
            @RequestParam Long userId) {

        return ResponseEntity.ok(
                interactionService.getInteractionStatus(userId, videoId).orElse(null)
        );
    }
}