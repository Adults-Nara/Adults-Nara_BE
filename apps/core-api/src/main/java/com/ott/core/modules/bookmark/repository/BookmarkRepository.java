package com.ott.core.modules.bookmark.repository;

import com.ott.common.persistence.entity.Bookmark;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.enums.VideoType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    // 1. 객체로 찾기 (북마크 추가/취소 시 영속성 컨텍스트 활용)
    Optional<Bookmark> findByUserAndVideoMetadata(User user, VideoMetadata videoMetadata);

    @Query("SELECT b FROM Bookmark b " +
            "JOIN FETCH b.videoMetadata vm " +  // FETCH JOIN
            "WHERE b.user.id = :userId AND vm.videoId = :videoId") // ID로 바로 비교
    Optional<Bookmark> findByUserIdAndVideoId(@Param("userId") Long userId, @Param("videoId") Long videoId);

    boolean existsByUserIdAndVideoMetadata_VideoId(Long userId, Long videoId);

    @Query("""
             SELECT vm.thumbnailUrl FROM Bookmark b
             JOIN b.videoMetadata vm
             WHERE b.user.id = :userId
                 AND vm.videoType = :videoType
                 AND vm.deleted = false
             ORDER BY b.createdAt DESC
            """)
    List<String> findThumbnailByUserIdAndVideoType(@Param("userId") Long userId, @Param("videoType") VideoType videoType, Pageable pageable);

    @Query("""
             SELECT COUNT(b) FROM Bookmark b
             JOIN b.videoMetadata vm
             WHERE b.user.id = :userId
                AND vm.videoType = :videoType
                AND vm.deleted = false
            """)
    long countByUserIdAndVideoType(@Param("userId") Long userId, @Param("videoType") VideoType videoType);

    @Query("""
            SELECT b FROM Bookmark b
            JOIN FETCH b.videoMetadata vm
            WHERE b.user.id = :userId
                AND vm.videoType = :videoType
                AND vm.deleted = false
            ORDER BY b.createdAt DESC
            """)
    Slice<Bookmark> findByUserIdAndVideoType(@Param("userId") Long userId, @Param("videoType") VideoType videoType, Pageable pageable);

    // 2. 존재 여부 확인 (video_metadata와 JOIN 하여 videoId 필터링)
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END " +
            "FROM Bookmark b JOIN b.videoMetadata vm " +
            "WHERE b.user.id = :userId AND vm.videoId = :videoId")
    boolean existsByUserIdAndVideoId(@Param("userId") Long userId, @Param("videoId") Long videoId);

    // 3. 특정 비디오의 북마크 총개수 계산 (명시적 JOIN)
    @Query("SELECT COUNT(b) FROM Bookmark b JOIN b.videoMetadata vm WHERE vm.videoId = :videoId")
    long countByVideoId(@Param("videoId") Long videoId);
}