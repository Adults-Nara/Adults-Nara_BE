package com.ott.core.modules.video.repository;

import com.ott.common.persistence.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Video v SET v.deleted = true WHERE v.id IN :ids")
    void softDeleteByIds(@Param("ids") List<Long> ids);
}
