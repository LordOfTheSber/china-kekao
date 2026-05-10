package dev.kekao.deck;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeckHanziRepository extends JpaRepository<DeckHanziEntity, DeckHanziId> {
    List<DeckHanziEntity> findByDeckIdOrderByPositionAsc(Long deckId);

    long countByDeckId(Long deckId);

    java.util.Optional<DeckHanziEntity> findByDeckIdAndHanziId(Long deckId, Long hanziId);

    @org.springframework.data.jpa.repository.Query("select coalesce(max(d.position), -1) from DeckHanziEntity d where d.deck.id = :deckId")
    int findMaxPositionByDeckId(@org.springframework.data.repository.query.Param("deckId") Long deckId);

    void deleteByDeckIdAndHanziId(Long deckId, Long hanziId);
}
