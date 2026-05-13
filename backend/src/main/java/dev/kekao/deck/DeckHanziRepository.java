package dev.kekao.deck;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DeckHanziRepository extends JpaRepository<DeckHanziEntity, DeckHanziId> {

    @Query("""
            SELECT d FROM DeckHanziEntity d
            JOIN FETCH d.hanzi
            WHERE d.deck.id = :deckId
            ORDER BY d.position ASC
            """)
    List<DeckHanziEntity> findByDeckIdOrderByPositionAsc(@Param("deckId") Long deckId);

    long countByDeckId(Long deckId);

    java.util.Optional<DeckHanziEntity> findByDeckIdAndHanziId(Long deckId, Long hanziId);

    @Query("select d.hanzi.id from DeckHanziEntity d where d.deck.id = :deckId")
    java.util.Set<Long> findHanziIdsByDeckId(@Param("deckId") Long deckId);

    @Query("select coalesce(max(d.position), -1) from DeckHanziEntity d where d.deck.id = :deckId")
    int findMaxPositionByDeckId(@Param("deckId") Long deckId);

    void deleteByDeckIdAndHanziId(Long deckId, Long hanziId);

    /**
     * Repacks positions to be consecutive starting from 0, ordered by current position then hanzi id.
     * A single SQL statement avoids issuing one UPDATE per remaining row.
     */
    @Modifying
    @Query(value = """
            WITH ordered AS (
                SELECT deck_id, hanzi_id,
                       ROW_NUMBER() OVER (ORDER BY position ASC, hanzi_id ASC) - 1 AS new_pos
                FROM deck_hanzi
                WHERE deck_id = :deckId
            )
            UPDATE deck_hanzi dh
            SET position = ordered.new_pos
            FROM ordered
            WHERE dh.deck_id = ordered.deck_id
              AND dh.hanzi_id = ordered.hanzi_id
              AND dh.position <> ordered.new_pos
            """, nativeQuery = true)
    int repackPositions(@Param("deckId") Long deckId);

    @Query("select d.deck.id, count(d) from DeckHanziEntity d where d.deck.id in :deckIds group by d.deck.id")
    List<Object[]> countsByDeckIds(@Param("deckIds") Collection<Long> deckIds);
}
