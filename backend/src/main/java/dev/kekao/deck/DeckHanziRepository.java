package dev.kekao.deck;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeckHanziRepository extends JpaRepository<DeckHanziEntity, DeckHanziId> {
    List<DeckHanziEntity> findByDeckIdOrderByPositionAsc(Long deckId);

    long countByDeckId(Long deckId);
}
