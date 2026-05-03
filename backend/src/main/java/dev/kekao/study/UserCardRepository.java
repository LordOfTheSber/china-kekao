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
     * Cards in subscribed decks that are due for review (any state except {@code NEW}).
     * Ordered by due date ascending so the most overdue surface first.
     */
    @Query("""
            SELECT uc FROM UserCardEntity uc
            WHERE uc.user.id = :userId
              AND uc.state <> :excludedState
              AND uc.dueDate <= :now
              AND EXISTS (
                  SELECT 1 FROM DeckHanziEntity dh, UserDeckEntity ud
                  WHERE dh.deck.id = ud.deck.id
                    AND dh.hanzi.id = uc.hanzi.id
                    AND ud.user.id = :userId
              )
            ORDER BY uc.dueDate ASC, uc.id ASC
            """)
    List<UserCardEntity> findDueReviewCardsForUser(@Param("userId") Long userId,
                                                   @Param("excludedState") CardState excludedState,
                                                   @Param("now") Instant now,
                                                   Pageable pageable);

    /**
     * Cards in the given state that belong to the user's subscribed decks. Used to surface
     * NEW-state cards for the daily queue.
     */
    @Query("""
            SELECT uc FROM UserCardEntity uc
            WHERE uc.user.id = :userId
              AND uc.state = :state
              AND EXISTS (
                  SELECT 1 FROM DeckHanziEntity dh, UserDeckEntity ud
                  WHERE dh.deck.id = ud.deck.id
                    AND dh.hanzi.id = uc.hanzi.id
                    AND ud.user.id = :userId
              )
            ORDER BY uc.id ASC
            """)
    List<UserCardEntity> findCardsInStateForUser(@Param("userId") Long userId,
                                                 @Param("state") CardState state,
                                                 Pageable pageable);

    @Query("""
            SELECT COUNT(uc) FROM UserCardEntity uc
            WHERE uc.user.id = :userId
              AND uc.state <> :excludedState
              AND uc.dueDate <= :now
              AND EXISTS (
                  SELECT 1 FROM DeckHanziEntity dh, UserDeckEntity ud
                  WHERE dh.deck.id = ud.deck.id
                    AND dh.hanzi.id = uc.hanzi.id
                    AND ud.user.id = :userId
              )
            """)
    long countDueReviewCardsForUser(@Param("userId") Long userId,
                                    @Param("excludedState") CardState excludedState,
                                    @Param("now") Instant now);

    @Query("""
            SELECT COUNT(uc) FROM UserCardEntity uc
            WHERE uc.user.id = :userId
              AND uc.state = :state
              AND EXISTS (
                  SELECT 1 FROM DeckHanziEntity dh, UserDeckEntity ud
                  WHERE dh.deck.id = ud.deck.id
                    AND dh.hanzi.id = uc.hanzi.id
                    AND ud.user.id = :userId
              )
            """)
    long countCardsInStateForUser(@Param("userId") Long userId, @Param("state") CardState state);

    long countByUserIdAndState(Long userId, CardState state);
}
