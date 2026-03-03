package com.ott.core.modules.comment.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CommentPageResponse {

    private List<CommentItemResponse> comments;
    private boolean hasMore;
    private CommentItemResponse myComment; // null이면 내 댓글 없음
}
