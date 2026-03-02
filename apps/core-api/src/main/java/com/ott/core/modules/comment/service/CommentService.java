package com.ott.core.modules.comment.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.Comment;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.core.modules.comment.dto.*;
import com.ott.core.modules.comment.repository.CommentRepository;
import com.ott.core.modules.user.repository.UserRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final VideoMetadataRepository videoMetadataRepository;

    public CommentPageResponse getComments(long userId, Long videoId, int page, int size) {
        Slice<Comment> slice = commentRepository.findByVideoId(videoId, PageRequest.of(page, size));

        List<CommentItemResponse> comments = slice.getContent().stream()
                .map(CommentItemResponse::from)
                .toList();

        CommentItemResponse myComment = commentRepository.findByUserIdAndVideoId(userId, videoId)
                .map(CommentItemResponse::from)
                .orElse(null);

        return CommentPageResponse.builder()
                .comments(comments)
                .hasMore(slice.hasNext())
                .myComment(myComment)
                .build();
    }

    public MyCommentResponse getMyComment(long userId, Long videoId) {
        return commentRepository.findByUserIdAndVideoId(userId, videoId)
                .map(MyCommentResponse::from)
                .orElse(null);
    }

    @Transactional
    public MyCommentResponse createComment(long userId, Long videoId, CommentCreateRequest request) {
        if (commentRepository.existsByUserIdAndVideoId(userId, videoId)) {
            throw new BusinessException(ErrorCode.COMMENT_ALREADY_EXISTS);
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        VideoMetadata videoMetadata = videoMetadataRepository.findByVideoId(videoId).orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));

        Comment comment = new Comment(videoMetadata, user, request.getText());
        commentRepository.save(comment);

        syncCommentCount(videoId);

        return MyCommentResponse.from(comment);
    }

    @Transactional
    public MyCommentResponse editComment(long userId, Long commentId, CommentEditRequest request) {
        Comment comment = findComment(commentId);

        if (!comment.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.COMMENT_FORBIDDEN);
        }

        comment.edit(request.getText());

        return MyCommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(long userId, Long commentId) {
        Comment comment = findComment(commentId);

        if (!comment.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.COMMENT_FORBIDDEN);
        }

        Long videoId = comment.getVideoMetadata().getVideoId();
        comment.softDelete();

        syncCommentCount(videoId);
    }

    private void syncCommentCount(Long videoId) {
        int count = commentRepository.countByVideoId(videoId);
        commentRepository.updateCommentCount(videoId, count);
    }

    private Comment findComment(Long commentId) {
        return commentRepository.findById(commentId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }
}
