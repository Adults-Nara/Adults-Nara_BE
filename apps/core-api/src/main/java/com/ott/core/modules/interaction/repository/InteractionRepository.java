package com.ott.core.modules.interaction.repository;

import com.ott.common.persistence.entity.Interaction;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.VideoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {

    // 유저와 비디오 정보를 가지고 좋아요/싫어요 기록을 찾음
    Optional<Interaction> findByUserAndVideoMetadata(User user, VideoMetadata videoMetadata);

    // JPQL을 사용하여 ID로 바로 조회 (성능 + 정확성 둘 다 잡음)
    @Query("select i from Interaction i where i.user.id = :userId and i.videoMetadata.id = :videoId")
    Optional<Interaction> findByUserIdAndVideoId(@Param("userId") Long userId, @Param("videoId") Long videoId);
}