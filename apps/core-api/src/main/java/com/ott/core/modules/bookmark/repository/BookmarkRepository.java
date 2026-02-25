package com.ott.core.modules.bookmark.repository;

import com.ott.common.persistence.entity.Bookmark;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.VideoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    // 1. 객체로 찾기 (Toggle용 - 쓰기)
    Optional<Bookmark> findByUserAndVideoMetadata(User user, VideoMetadata videoMetadata);

    @Query("SELECT b FROM Bookmark b " +
            "JOIN FETCH b.videoMetadata vm " +  // FETCH JOIN
            "WHERE b.user.id = :userId AND vm.videoId = :videoId") // ID로 바로 비교
    Optional<Bookmark> findByUserIdAndVideoId(@Param("userId") Long userId, @Param("videoId") Long videoId);

    boolean existsByUserIdAndVideoMetadata_VideoId(Long userId, Long videoId);
}