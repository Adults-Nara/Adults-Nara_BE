package com.ott.batch.modules.User;

import com.ott.common.persistence.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    @Modifying
    @Query(value = """
            INSERT INTO user_preference (user_preference_id, user_id, tag_id, score, created_at, updated_at)
            VALUES (:id, :userId, :tagId, :addScore, :now, :now)
            ON CONFLICT (user_id, tag_id) 
            DO UPDATE SET
                score = user_preference.score + :addScore,
                updated_at = :now
            """, nativeQuery = true)
    void addScore(@Param("id") Long id,
                  @Param("userId") Long userId,
                  @Param("tagId") Long tagId,
                  @Param("addScore") Double addScore,
                  @Param("now") LocalDateTime now);
}
