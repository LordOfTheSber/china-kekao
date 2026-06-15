package dev.kekao.study;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Date;
import java.time.Instant;
import java.util.List;

public interface ReviewLogRepository extends JpaRepository<ReviewLogEntity, Long> {
    List<ReviewLogEntity> findByUserCardIdOrderByReviewedAtAsc(Long userCardId);

    @Query("SELECT COUNT(rl) FROM ReviewLogEntity rl WHERE rl.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT rl.reviewedAt, rl.rating FROM ReviewLogEntity rl
            WHERE rl.user.id = :userId
              AND rl.reviewedAt >= :since
            ORDER BY rl.reviewedAt DESC
            """)
    List<Object[]> findUserReviewsSince(@Param("userId") Long userId, @Param("since") Instant since);

    @Query(value = """
            SELECT date_trunc('day', rl.reviewed_at AT TIME ZONE 'UTC')::date AS day,
                   COUNT(*) AS total,
                   SUM(CASE WHEN rl.rating >= 3 THEN 1 ELSE 0 END) AS good
            FROM review_log rl
            WHERE rl.user_id = :userId
              AND rl.reviewed_at >= :since
            GROUP BY day
            ORDER BY day ASC
            """, nativeQuery = true)
    List<Object[]> findDailyReviewCounts(@Param("userId") Long userId, @Param("since") Instant since);

    /**
     * Distinct UTC days on which the user recorded at least one review since {@code since}.
     * Lets the streak computation skip loading every individual log row.
     */
    @Query(value = """
            SELECT DISTINCT date_trunc('day', rl.reviewed_at AT TIME ZONE 'UTC')::date
            FROM review_log rl
            WHERE rl.user_id = :userId
              AND rl.reviewed_at >= :since
            """, nativeQuery = true)
    List<Date> findDistinctReviewDays(@Param("userId") Long userId, @Param("since") Instant since);
}
