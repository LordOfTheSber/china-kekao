package dev.kekao.study;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserCardRepository extends JpaRepository<UserCardEntity, Long> {

    Optional<UserCardEntity> findByUserIdAndHanziIdAndMode(Long userId, Long hanziId, StudyMode mode);

    List<UserCardEntity> findByUserIdAndDueDateLessThanEqual(Long userId, Instant due);

    /**
     * Cards in subscribed decks that are due for review (any non-NEW state).
     * Ordered by due date ascending so the most overdue surface first.
     */
    @Query("""
            SELECT uc FROM UserCardEntity uc
            WHERE uc.user.id = :userId
              AND uc.state <> dev.kekao.study.CardState.NEW
              AND uc.dueDate <= :now
              AND uc.hanzi.id IN (
                  SELECT dh.hanzi.id FROM DeckHanziEntity dh
                  WHERE dh.deck.id IN (
                      SELECT ud.deck.id FROM UserDeckEntity ud
                      WHERE ud.user.id = :userId
                  )
              )
            ORDER BY uc.dueDate ASC, uc.id ASC
            """)
    List<UserCardEntity> findDueReviewCardsForUser(@Param("userId") Long userId,
                                                   @Param("now") Instant now,
                                                   Pageable pageable);

    /**
     * NEW-state cards in subscribed decks, ordered by deck position then id so the queue
     * is stable across calls.
     */
    @Query("""
            SELECT uc FROM UserCardEntity uc
            WHERE uc.user.id = :userId
              AND uc.state = dev.kekao.study.CardState.NEW
              AND uc.hanzi.id IN (
                  SELECT dh.hanzi.id FROM DeckHanziEntity dh
                  WHERE dh.deck.id IN (
                      SELECT ud.deck.id FROM UserDeckEntity ud
                      WHERE ud.user.id = :userId
                  )
              )
            ORDER BY uc.id ASC
            """)
    List<UserCardEntity> findNewCardsForUser(@Param("userId") Long userId, Pageable pageable);
}
