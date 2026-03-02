package com.ott.core.modules.comment.repository;

import com.ott.common.persistence.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 특정 영상의 삭제되지 않은 댓글 목록 (무한 스크롤)
    @Query("SELECT c FROM Comment c WHERE c.videoMetadata.videoId = :videoId AND c.deleted = false ORDER BY c.createdAt DESC")
    Slice<Comment> findByVideoId(@Param("videoId") Long videoId, Pageable pageable);

    // 특정 사용자의 특정 영상 댓글 조회
    @Query("SELECT c FROM Comment c WHERE c.user.id = :userId AND c.videoMetadata.videoId = :videoId AND c.deleted = false")
    Optional<Comment> findByUserIdAndVideoId(@Param("userId") Long userId, @Param("videoId") Long videoId);

    // 댓글 작성 여부 확인
    @Query("SELECT COUNT(c) > 0 FROM Comment c WHERE c.user.id = :userId AND c.videoMetadata.videoId = :videoId AND c.deleted = false")
    boolean existsByUserIdAndVideoId(@Param("userId") Long userId, @Param("videoId") Long videoId);

    // 댓글 수 카운트 (VideoMetadata 업데이트용)
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.videoMetadata.videoId = :videoId AND c.deleted = false")
    int countByVideoId(@Param("videoId") Long videoId);

    // videoMetadata의 commentCount 업데이트
    @Modifying(clearAutomatically = true)
    @Query("UPDATE VideoMetadata v SET v.commentCount = :count WHERE v.videoId = :videoId")
    void updateCommentCount(@Param("videoId") Long videoId, @Param("count") int count);
}
