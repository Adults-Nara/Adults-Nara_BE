package com.ott.core.modules.tag.repository;

import com.ott.common.persistence.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {

    @Query("SELECT t FROM Tag t WHERE t.parent IS NULL ORDER BY t.tagName")
    List<Tag> findAllParentTags();

    @Query("SELECT t FROM Tag t JOIN FETCH t.parent WHERE t.parent IS NOT NULL ORDER BY t.parent.id, t.tagName")
    List<Tag> findAllChildTagsWithParent();
}
