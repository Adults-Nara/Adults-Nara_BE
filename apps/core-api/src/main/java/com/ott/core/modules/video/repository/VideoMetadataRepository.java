package com.ott.core.modules.video.repository;

import com.ott.common.persistence.entity.VideoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VideoMetadataRepository extends JpaRepository<VideoMetadata, Long> {

    Optional<VideoMetadata> findByVideoIdAndDeleted(Long videoId, boolean deleted);

    // ================= [좋아요] =================
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE VideoMetadata v SET v.likeCount = v.likeCount + 1 WHERE v.id = :id")
    void increaseLikeCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE VideoMetadata v SET v.likeCount = v.likeCount - 1 WHERE v.id = :id AND v.likeCount > 0")
    void decreaseLikeCount(@Param("id") Long id);


    // ================= [싫어요] =================
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE VideoMetadata v SET v.dislikeCount = v.dislikeCount + 1 WHERE v.id = :id")
    void increaseDislikeCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE VideoMetadata v SET v.dislikeCount = v.dislikeCount - 1 WHERE v.id = :id AND v.dislikeCount > 0")
    void decreaseDislikeCount(@Param("id") Long id);


    // ================= [왕따봉] =================
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE VideoMetadata v SET v.superLikeCount = v.superLikeCount + 1 WHERE v.id = :id")
    void increaseSuperLikeCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE VideoMetadata v SET v.superLikeCount = v.superLikeCount - 1 WHERE v.id = :id AND v.superLikeCount > 0")
    void decreaseSuperLikeCount(@Param("id") Long id);


    // ================= [북마크] =================
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE VideoMetadata v SET v.bookmarkCount = v.bookmarkCount + 1 WHERE v.id = :id")
    void increaseBookmarkCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE VideoMetadata v SET v.bookmarkCount = v.bookmarkCount - 1 WHERE v.id = :id AND v.bookmarkCount > 0")
    void decreaseBookmarkCount(@Param("id") Long id);
}
