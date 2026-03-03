package com.ott.core.modules.comment.dto;

import com.ott.common.persistence.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class MyCommentResponse {

    private String commentId;
    private String text;
    private String nickname;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static MyCommentResponse from(Comment comment) {
        return MyCommentResponse.builder()
                .commentId(String.valueOf(comment.getId()))
                .text(comment.getText())
                .nickname(comment.getUser().getNickname())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
