package com.ott.core.modules.preference.repository;
import com.ott.common.persistence.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    /**
     * [점수 누적 Upsert 쿼리]
     * - 데이터가 없으면 INSERT
     * - 이미 있으면(ON CONFLICT) 기존 점수에 더하기(+)
     */
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

    @Query("SELECT up FROM UserPreference up JOIN FETCH up.tag WHERE up.user.id = :userId ORDER BY up.score DESC")
    List<UserPreference> findWithTagByUserId(@Param("userId") Long userId);
}