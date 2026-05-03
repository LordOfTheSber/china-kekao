package dev.kekao.study;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ReviewLogRepository extends JpaRepository<ReviewLogEntity, Long> {
    List<ReviewLogEntity> findByUserCardIdOrderByReviewedAtAsc(Long userCardId);

    @Query("""
            SELECT rl.reviewedAt, rl.rating FROM ReviewLogEntity rl
            WHERE rl.userCard.user.id = :userId
              AND rl.reviewedAt >= :since
            ORDER BY rl.reviewedAt DESC
            """)
    List<Object[]> findUserReviewsSince(@Param("userId") Long userId, @Param("since") Instant since);
}
