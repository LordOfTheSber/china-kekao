package dev.kekao.deck;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeckRepository extends JpaRepository<DeckEntity, Long> {
    Optional<DeckEntity> findBySlug(String slug);

    Optional<DeckEntity> findBySlugAndOwnerIsNull(String slug);

    Optional<DeckEntity> findBySlugAndOwnerId(String slug, Long ownerId);

    /**
     * System decks (visible to all) + the user's owned decks, in a single round-trip.
     * Ordered system-first, then by id for deterministic display.
     */
    @Query("""
            SELECT d FROM DeckEntity d
            WHERE d.system = TRUE OR d.owner.id = :userId
            ORDER BY d.system DESC, d.id ASC
            """)
    List<DeckEntity> findVisibleForUser(@Param("userId") Long userId);

    @Query("SELECT d.slug FROM DeckEntity d WHERE d.owner.id = :ownerId AND d.slug LIKE :prefix")
    List<String> findSlugsByOwnerIdStartingWith(@Param("ownerId") Long ownerId,
                                                @Param("prefix") String prefix);
}
