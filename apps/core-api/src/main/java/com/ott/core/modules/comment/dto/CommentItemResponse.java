package com.ott.core.modules.comment.dto;

import com.ott.common.persistence.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class CommentItemResponse {

    private String commentId;
    private String nickname;
    private String profileImageUrl;
    private String text;
    private OffsetDateTime createdAt;

    public static CommentItemResponse from(Comment comment) {
        return CommentItemResponse.builder()
                .commentId(String.valueOf(comment.getId()))
                .nickname(comment.getUser().getNickname())
                .profileImageUrl(comment.getUser().getProfileImageUrl())
                .text(comment.getText())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
