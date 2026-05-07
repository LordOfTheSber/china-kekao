package dev.kekao.deck;

import dev.kekao.hanzi.HanziEntity;
import dev.kekao.hanzi.HanziRepository;
import dev.kekao.hanzi.HanziStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ensures the catalogue contains a system deck per HSK level (1–6) and that
 * each such deck mirrors every PUBLISHED hanzi at that level.
 *
 * <p>Deck membership is reconciled on every startup so newly published or
 * delisted hanzi propagate to the user-facing decks list without manual
 * intervention.</p>
 */
@Component
@Order(2)
public class DeckBootstrap {

    static final List<Short> HSK_LEVELS = List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6);

    private static final Logger log = LoggerFactory.getLogger(DeckBootstrap.class);

    private final DeckRepository decks;
    private final DeckHanziRepository deckHanzi;
    private final HanziRepository hanzi;

    @Autowired
    public DeckBootstrap(DeckRepository decks, DeckHanziRepository deckHanzi, HanziRepository hanzi) {
        this.decks = decks;
        this.deckHanzi = deckHanzi;
        this.hanzi = hanzi;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            ensureSystemDecks();
        } catch (RuntimeException ex) {
            log.warn("HSK deck bootstrap failed; will retry on next startup", ex);
        }
    }

    @Transactional
    public void ensureSystemDecks() {
        for (Short level : HSK_LEVELS) {
            DeckEntity deck = ensureDeck(level);
            syncMembers(deck, level);
        }
    }

    private DeckEntity ensureDeck(Short level) {
        String slug = "hsk-" + level;
        return decks.findBySlug(slug).orElseGet(() -> {
            DeckEntity created = decks.save(DeckEntity.builder()
                    .name("HSK " + level)
                    .slug(slug)
                    .system(true)
                    .description("All HSK " + level + " characters that have been published.")
                    .build());
            log.info("Created system deck {}", slug);
            return created;
        });
    }

    private void syncMembers(DeckEntity deck, Short level) {
        List<HanziEntity> published = hanzi.findAllByStatusAndHskLevelOrderByFrequencyRankAscIdAsc(
                HanziStatus.PUBLISHED, level);
        Set<Long> publishedIds = published.stream().map(HanziEntity::getId).collect(Collectors.toSet());

        List<DeckHanziEntity> existing = deckHanzi.findByDeckIdOrderByPositionAsc(deck.getId());
        Map<Long, DeckHanziEntity> existingByHanzi = existing.stream()
                .collect(Collectors.toMap(e -> e.getHanzi().getId(), e -> e, (a, b) -> a));

        // Remove members no longer published.
        Set<Long> seen = new HashSet<>();
        for (DeckHanziEntity entry : existing) {
            Long hanziId = entry.getHanzi().getId();
            if (!publishedIds.contains(hanziId) || !seen.add(hanziId)) {
                deckHanzi.delete(entry);
            }
        }

        // Add missing and re-position so frequency_rank ascending becomes deck order.
        Map<Long, Integer> targetPositions = new HashMap<>();
        for (int i = 0; i < published.size(); i++) {
            targetPositions.put(published.get(i).getId(), i);
        }
        for (HanziEntity h : published) {
            DeckHanziEntity entry = existingByHanzi.get(h.getId());
            int position = targetPositions.get(h.getId());
            if (entry == null) {
                deckHanzi.save(DeckHanziEntity.builder()
                        .id(new DeckHanziId(deck.getId(), h.getId()))
                        .deck(deck)
                        .hanzi(h)
                        .position(position)
                        .build());
            } else if (entry.getPosition() == null || entry.getPosition() != position) {
                entry.setPosition(position);
                deckHanzi.save(entry);
            }
        }
    }
}
