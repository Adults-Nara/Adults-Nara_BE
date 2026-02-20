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

    /**
     * ✅ [추가] N+1 방지: 여러 비디오 ID를 IN 절로 던져서 연관된 태그를 한 번에 가져옵니다.
     * (JOIN FETCH를 써서 Tag 테이블까지 쿼리 한 방에 가져옵니다)
     */
    @Query("SELECT vt FROM VideoTag vt JOIN FETCH vt.tag WHERE vt.videoMetadata.id IN :videoIds")
    List<VideoTag> findWithTagByVideoMetadataIdIn(@Param("videoIds") List<Long> videoIds);
}