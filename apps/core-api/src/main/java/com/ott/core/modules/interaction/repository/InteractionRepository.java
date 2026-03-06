package com.ott.core.modules.interaction.repository;

import com.ott.common.persistence.entity.Interaction;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.enums.InteractionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {

    // 객체로 찾기 (인터랙션 추가/수정/취소용)
    Optional<Interaction> findByUserAndVideoMetadata(User user, VideoMetadata videoMetadata);

    // ID로 찾기 (video_metadata와 조인하여 조회)
    @Query("SELECT i FROM Interaction i JOIN FETCH i.videoMetadata vm " +
            "WHERE i.user.id = :userId AND vm.videoId = :videoId")
    Optional<Interaction> findByUserIdAndVideoId(@Param("userId") Long userId, @Param("videoId") Long videoId);

    // 특정 비디오의 특정 타입(좋아요/싫어요) 총개수 계산 (명시적 JOIN)
    @Query("SELECT COUNT(i) FROM Interaction i JOIN i.videoMetadata vm " +
            "WHERE vm.videoId = :videoId AND i.interactionType = :type")
    long countByVideoIdAndType(@Param("videoId") Long videoId, @Param("type") InteractionType type);
}