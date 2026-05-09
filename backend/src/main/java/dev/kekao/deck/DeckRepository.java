package dev.kekao.deck;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeckRepository extends JpaRepository<DeckEntity, Long> {
    Optional<DeckEntity> findBySlug(String slug);

    Optional<DeckEntity> findBySlugAndOwnerIsNull(String slug);

    Optional<DeckEntity> findBySlugAndOwnerId(String slug, Long ownerId);

    List<DeckEntity> findAllBySystemTrueOrderByIdAsc();

    List<DeckEntity> findAllByOwnerIdOrderByIdAsc(Long ownerId);
}
