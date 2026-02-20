package com.ott.core.modules.tag.repository;

import com.ott.common.persistence.entity.Tag;
import com.ott.common.persistence.entity.VideoTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface VideoTagRepository extends JpaRepository<VideoTag, Long> {

    // [성능 최적화] Fetch Join을 사용해 VideoTag와 Tag를 한 번의 쿼리로 가져옴
    @Query("SELECT vt.tag FROM VideoTag vt JOIN vt.tag WHERE vt.videoMetadata.id = :videoId")
    List<Tag> findTagsByVideoId(@Param("videoId") Long videoId);
}