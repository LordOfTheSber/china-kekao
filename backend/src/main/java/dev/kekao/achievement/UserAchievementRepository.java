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

    boolean existsByUserIdAndAchievementId(Long userId, Long achievementId);
}
