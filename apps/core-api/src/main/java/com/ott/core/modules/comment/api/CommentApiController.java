package com.ott.core.modules.comment.api;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.comment.dto.CommentCreateRequest;
import com.ott.core.modules.comment.dto.CommentEditRequest;
import com.ott.core.modules.comment.dto.CommentPageResponse;
import com.ott.core.modules.comment.dto.MyCommentResponse;
import com.ott.core.modules.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/comment")
public class CommentApiController {

    private final CommentService commentService;

    // 댓글 목록 조회
    @GetMapping("/videos/{videoId}")
    public ApiResponse<CommentPageResponse> getComments(
            @AuthenticationPrincipal String userId,
            @PathVariable("videoId") Long videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        CommentPageResponse response = commentService.getComments(Long.parseLong(userId), videoId, page, size);
        return ApiResponse.success(response);
    }

    // 내 댓글 조회
    @GetMapping("/videos/{videoId}/me")
    public ApiResponse<MyCommentResponse> getMyComment(
            @AuthenticationPrincipal String userId,
            @PathVariable("videoId") Long videoId
    ) {
        MyCommentResponse response = commentService.getMyComment(Long.parseLong(userId), videoId);
        return ApiResponse.success(response);
    }

    // 댓글 작성
    @PostMapping("/videos/{videoId}")
    public ApiResponse<MyCommentResponse> createComment(
            @AuthenticationPrincipal String userId,
            @PathVariable("videoId") Long videoId,
            @RequestBody @Valid CommentCreateRequest request
    ) {
        MyCommentResponse response = commentService.createComment(Long.parseLong(userId), videoId, request);
        return ApiResponse.success(response);
    }

    // 댓글 수정
    @PatchMapping("/{commentId}")
    public ApiResponse<MyCommentResponse> editComment(
            @AuthenticationPrincipal String userId,
            @PathVariable("commentId") Long commentId,
            @RequestBody @Valid CommentEditRequest request
    ) {
        MyCommentResponse response = commentService.editComment(Long.parseLong(userId), commentId, request);
        return ApiResponse.success(response);
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ApiResponse<?> deleteComment(
            @AuthenticationPrincipal String userId,
            @PathVariable("commentId") Long commentId
    ) {
        commentService.deleteComment(Long.parseLong(userId), commentId);
        return ApiResponse.success();
    }
}
