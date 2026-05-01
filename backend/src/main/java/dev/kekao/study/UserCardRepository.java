package dev.kekao.study;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserCardRepository extends JpaRepository<UserCardEntity, Long> {

    Optional<UserCardEntity> findByUserIdAndHanziIdAndMode(Long userId, Long hanziId, StudyMode mode);

    List<UserCardEntity> findByUserIdAndDueDateLessThanEqual(Long userId, Instant due);
}
