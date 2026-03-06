package com.ott.core.modules.video.repository;

import com.ott.common.persistence.entity.Video;
import com.ott.common.persistence.enums.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Video v SET v.deleted = true WHERE v.id IN :ids")
    void softDeleteByIds(@Param("ids") List<Long> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Video v SET v.visibility = :visibility, v.updatedAt = :now, " +
            "v.publishedAt = CASE WHEN :visibility = com.ott.common.persistence.enums.Visibility.PUBLIC THEN :now ELSE v.publishedAt END " +
            "WHERE v.id IN :ids")
    void updateVisibilityByIds(@Param("visibility") Visibility visibility, @Param("now") OffsetDateTime now, @Param("ids") List<Long> ids);
}
