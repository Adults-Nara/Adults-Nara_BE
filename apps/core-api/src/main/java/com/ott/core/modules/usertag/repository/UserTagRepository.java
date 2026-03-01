package com.ott.core.modules.usertag.repository;

import com.ott.common.persistence.entity.Tag;
import com.ott.common.persistence.entity.UserTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserTagRepository extends JpaRepository<UserTag, Long> {

    @Query("SELECT ut.tag FROM UserTag ut JOIN ut.tag t WHERE ut.user.id = :userId AND t.parent IS NOT NULL")
    List<Tag> findChildTagsByUserId(@Param("userId") Long userId);

    @Query("SELECT ut.tag.id FROM UserTag ut WHERE ut.user.id = :userId")
    List<Long> findTagIdsByUserId(@Param("userId") Long userId);

    void deleteByUserIdAndTagIdIn(Long userId, List<Long> tagIds);
}
