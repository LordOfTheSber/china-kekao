package dev.kekao.study;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ReviewLogRepository extends JpaRepository<ReviewLogEntity, Long> {
    List<ReviewLogEntity> findByUserCardIdOrderByReviewedAtAsc(Long userCardId);

    @Query("SELECT COUNT(rl) FROM ReviewLogEntity rl WHERE rl.userCard.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT rl.reviewedAt, rl.rating FROM ReviewLogEntity rl
            WHERE rl.userCard.user.id = :userId
              AND rl.reviewedAt >= :since
            ORDER BY rl.reviewedAt DESC
            """)
    List<Object[]> findUserReviewsSince(@Param("userId") Long userId, @Param("since") Instant since);

    @Query(value = """
            SELECT date_trunc('day', rl.reviewed_at AT TIME ZONE 'UTC')::date AS day,
                   COUNT(*) AS total,
                   SUM(CASE WHEN rl.rating >= 3 THEN 1 ELSE 0 END) AS good
            FROM review_log rl
            JOIN user_card uc ON uc.id = rl.user_card_id
            WHERE uc.user_id = :userId
              AND rl.reviewed_at >= :since
            GROUP BY day
            ORDER BY day ASC
            """, nativeQuery = true)
    List<Object[]> findDailyReviewCounts(@Param("userId") Long userId, @Param("since") Instant since);
}
