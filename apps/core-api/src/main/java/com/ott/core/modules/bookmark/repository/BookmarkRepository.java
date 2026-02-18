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

    // 2. ID로 찾기 (조회용 - 읽기)
    @Query("select b from Bookmark b where b.user.id = :userId and b.videoMetadata.id = :videoId")
    Optional<Bookmark> findByUserIdAndVideoId(@Param("userId") Long userId, @Param("videoId") Long videoId);
}