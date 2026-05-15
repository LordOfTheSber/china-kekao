package dev.kekao.achievement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AchievementRepository extends JpaRepository<AchievementEntity, Long> {
    Optional<AchievementEntity> findByCode(String code);

    List<AchievementEntity> findByCategoryOrderBySortOrderAsc(AchievementCategory category);

    List<AchievementEntity> findAllByOrderBySortOrderAsc();
}
