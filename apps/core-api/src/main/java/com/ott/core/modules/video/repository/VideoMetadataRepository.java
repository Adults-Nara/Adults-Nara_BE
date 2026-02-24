package com.ott.core.modules.video.repository;

import com.ott.common.persistence.entity.VideoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VideoMetadataRepository extends JpaRepository<VideoMetadata, Long> {

    Optional<VideoMetadata> findByVideoId(Long videoId);

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
    @Modifying(clearAutomatically = true)
    @Query("UPDATE VideoMetadata v SET v.bookmarkCount = :count WHERE v.videoId = :videoId")
    void updateBookmarkCount(@Param("videoId") Long videoId, @Param("count") int count);

    // COUNT 쿼리 없이 딱 (요청한 사이즈 + 1)개만 가져와서 다음 페이지 여부만 판단하는 Slice
    org.springframework.data.domain.Slice<VideoMetadata> findSliceBy(org.springframework.data.domain.Pageable pageable);

    // ADMIN 전용 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE VideoMetadata vm SET vm.deleted = true WHERE vm.videoId IN :ids")
    void softDeleteByAdmin(@Param("ids") List<Long> ids);

    // UPLOADER 전용 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE VideoMetadata vm SET vm.deleted = true WHERE vm.videoId IN :ids AND vm.userId = :userId")
    void softDeleteByUploader(@Param("ids") List<Long> ids, @Param("userId") Long userId);
}
