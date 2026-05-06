package dev.kekao.deck;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeckRepository extends JpaRepository<DeckEntity, Long> {
    Optional<DeckEntity> findBySlug(String slug);

    java.util.List<DeckEntity> findAllBySystemTrueOrderByIdAsc();
}
