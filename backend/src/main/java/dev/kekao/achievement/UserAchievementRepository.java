package dev.kekao.achievement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface UserAchievementRepository extends JpaRepository<UserAchievementEntity, Long> {

    @Query("SELECT ua.achievement.id FROM UserAchievementEntity ua WHERE ua.user.id = :userId")
    Set<Long> findAchievementIdsForUser(@Param("userId") Long userId);

    @Query("SELECT ua FROM UserAchievementEntity ua "
            + "JOIN FETCH ua.achievement "
            + "WHERE ua.user.id = :userId "
            + "ORDER BY ua.unlockedAt DESC")
    List<UserAchievementEntity> findUnlockedForUser(@Param("userId") Long userId);

    /**
     * Locked threshold-based achievements for the given user, ordered for stable evaluation.
     * Filters in SQL so {@code checkAfterReview} can skip the full catalog scan + in-memory filter.
     */
    @Query("""
            SELECT a FROM AchievementEntity a
            WHERE a.threshold IS NOT NULL
              AND a.category <> dev.kekao.achievement.AchievementCategory.SPECIAL
              AND NOT EXISTS (
                  SELECT 1 FROM UserAchievementEntity ua
                  WHERE ua.achievement.id = a.id AND ua.user.id = :userId
              )
            ORDER BY a.sortOrder ASC
            """)
    List<AchievementEntity> findLockedThresholdAchievements(@Param("userId") Long userId);

    boolean existsByUserIdAndAchievementId(Long userId, Long achievementId);
}
