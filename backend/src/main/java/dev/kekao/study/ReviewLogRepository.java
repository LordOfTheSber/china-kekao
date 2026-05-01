package dev.kekao.study;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewLogRepository extends JpaRepository<ReviewLogEntity, Long> {
    List<ReviewLogEntity> findByUserCardIdOrderByReviewedAtAsc(Long userCardId);
}
