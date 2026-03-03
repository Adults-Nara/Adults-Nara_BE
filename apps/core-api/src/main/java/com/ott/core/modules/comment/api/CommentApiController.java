package com.ott.core.modules.comment.api;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.comment.dto.CommentCreateRequest;
import com.ott.core.modules.comment.dto.CommentEditRequest;
import com.ott.core.modules.comment.dto.CommentPageResponse;
import com.ott.core.modules.comment.dto.MyCommentResponse;
import com.ott.core.modules.comment.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/comment")
@Tag(name = "댓글 API", description = "댓글 조회, 작성, 수정, 삭제 API")
public class CommentApiController {

    private final CommentService commentService;

    @Operation(summary = "댓글 목록 조회", description = "특정 영상의 댓글 목록을 페이지네이션으로 조회합니다. 비로그인 사용자도 조회 가능합니다.")
    @GetMapping("/videos/{videoId}")
    public ApiResponse<CommentPageResponse> getComments(
            @AuthenticationPrincipal String userId,
            @PathVariable("videoId") Long videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long parsedUserId = userId != null ? Long.parseLong(userId) : null;
        CommentPageResponse response = commentService.getComments(parsedUserId, videoId, page, size);
        return ApiResponse.success(response);
    }

    @Operation(summary = "내 댓글 조회", description = "특정 영상에 내가 작성한 댓글을 조회합니다.")
    @GetMapping("/videos/{videoId}/me")
    public ApiResponse<MyCommentResponse> getMyComment(
            @AuthenticationPrincipal String userId,
            @PathVariable("videoId") Long videoId
    ) {
        MyCommentResponse response = commentService.getMyComment(Long.parseLong(userId), videoId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "댓글 작성", description = "특정 영상에 댓글을 작성합니다.")
    @PostMapping("/videos/{videoId}")
    public ApiResponse<MyCommentResponse> createComment(
            @AuthenticationPrincipal String userId,
            @PathVariable("videoId") Long videoId,
            @RequestBody @Valid CommentCreateRequest request
    ) {
        MyCommentResponse response = commentService.createComment(Long.parseLong(userId), videoId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "댓글 수정", description = "내가 작성한 댓글을 수정합니다.")
    @PatchMapping("/{commentId}")
    public ApiResponse<MyCommentResponse> editComment(
            @AuthenticationPrincipal String userId,
            @PathVariable("commentId") Long commentId,
            @RequestBody @Valid CommentEditRequest request
    ) {
        MyCommentResponse response = commentService.editComment(Long.parseLong(userId), commentId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "댓글 삭제", description = "내가 작성한 댓글을 삭제합니다.")
    @DeleteMapping("/{commentId}")
    public ApiResponse<?> deleteComment(
            @AuthenticationPrincipal String userId,
            @PathVariable("commentId") Long commentId
    ) {
        commentService.deleteComment(Long.parseLong(userId), commentId);
        return ApiResponse.success();
    }
}
