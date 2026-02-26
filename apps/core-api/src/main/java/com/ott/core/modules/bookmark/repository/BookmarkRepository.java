package com.ott.core.modules.bookmark.repository;

import com.ott.common.persistence.entity.Bookmark;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.VideoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    // 1. 객체로 찾기 (북마크 추가/취소 시 영속성 컨텍스트 활용)
    Optional<Bookmark> findByUserAndVideoMetadata(User user, VideoMetadata videoMetadata);

    // 2. 존재 여부 확인 (video_metadata와 JOIN 하여 videoId 필터링)
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END " +
            "FROM Bookmark b JOIN b.videoMetadata vm " +
            "WHERE b.user.id = :userId AND vm.videoId = :videoId")
    boolean existsByUserIdAndVideoId(@Param("userId") Long userId, @Param("videoId") Long videoId);

    // 3. 특정 비디오의 북마크 총개수 계산 (명시적 JOIN)
    @Query("SELECT COUNT(b) FROM Bookmark b JOIN b.videoMetadata vm WHERE vm.videoId = :videoId")
    long countByVideoId(@Param("videoId") Long videoId);
}