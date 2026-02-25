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

    List<VideoMetadata> findAllByVideoIdIn(List<Long> videoIds);

    Optional<VideoMetadata> findByVideoIdAndDeleted(Long videoId, boolean deleted);

    // ================= [Redis -> DB 동기화 용도 (Write-Back)] =================
    @Modifying(clearAutomatically = true)
    @Query("UPDATE VideoMetadata v SET v.likeCount = CASE WHEN :count < 0 THEN 0 ELSE :count END WHERE v.videoId = :videoId")
    void updateLikeCount(@Param("videoId") Long videoId, @Param("count") int count);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE VideoMetadata v SET v.dislikeCount = CASE WHEN :count < 0 THEN 0 ELSE :count END WHERE v.videoId = :videoId")
    void updateDislikeCount(@Param("videoId") Long videoId, @Param("count") int count);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE VideoMetadata v SET v.superLikeCount = CASE WHEN :count < 0 THEN 0 ELSE :count END WHERE v.videoId = :videoId")
    void updateSuperLikeCount(@Param("videoId") Long videoId, @Param("count") int count);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE VideoMetadata v SET v.bookmarkCount = CASE WHEN :count < 0 THEN 0 ELSE :count END WHERE v.videoId = :videoId")
    void updateBookmarkCount(@Param("videoId") Long videoId, @Param("count") int count);

    @Query("SELECT v FROM VideoMetadata v WHERE v.bookmarkCount > 0")
    List<VideoMetadata> findAllWithBookmarks();

    // =========================================================================

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
