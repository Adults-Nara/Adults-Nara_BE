package com.ott.core.modules.tag.repository;

import com.ott.common.persistence.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
}
