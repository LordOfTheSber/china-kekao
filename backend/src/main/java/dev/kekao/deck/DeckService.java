package dev.kekao.deck;

import dev.kekao.deck.DeckDtos.CreateDeckRequest;
import dev.kekao.deck.DeckDtos.DeckDetailView;
import dev.kekao.deck.DeckDtos.DeckHanziView;
import dev.kekao.deck.DeckDtos.DeckView;
import dev.kekao.deck.DeckDtos.SubscribeResponse;
import dev.kekao.deck.DeckDtos.UpdateDeckRequest;
import dev.kekao.hanzi.HanziEntity;
import dev.kekao.hanzi.HanziRepository;
import dev.kekao.hanzi.HanziStatus;
import dev.kekao.hanzi.HanziTranslationRepository;
import dev.kekao.study.CardState;
import dev.kekao.study.StudyMode;
import dev.kekao.study.UserCardEntity;
import dev.kekao.study.UserCardRepository;
import dev.kekao.user.UserEntity;
import dev.kekao.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class DeckService {

    private final DeckRepository decks;
    private final DeckHanziRepository deckHanzi;
    private final UserDeckRepository userDecks;
    private final UserCardRepository userCards;
    private final UserRepository users;
    private final HanziRepository hanzi;
    private final HanziTranslationRepository translations;

    @Autowired
    public DeckService(DeckRepository decks,
                       DeckHanziRepository deckHanzi,
                       UserDeckRepository userDecks,
                       UserCardRepository userCards,
                       UserRepository users,
                       HanziRepository hanzi,
                       HanziTranslationRepository translations) {
        this.decks = decks;
        this.deckHanzi = deckHanzi;
        this.userDecks = userDecks;
        this.userCards = userCards;
        this.users = users;
        this.hanzi = hanzi;
        this.translations = translations;
    }

    @Transactional(readOnly = true)
    public List<DeckView> listDecks(Long userId) {
        List<DeckEntity> all = decks.findVisibleForUser(userId);
        if (all.isEmpty()) return List.of();
        Set<Long> subscribed = new HashSet<>(userDecks.findDeckIdsByUserId(userId));
        List<Long> deckIds = all.stream().map(DeckEntity::getId).toList();
        Map<Long, Long> countByDeck = new HashMap<>();
        for (Object[] row : deckHanzi.countsByDeckIds(deckIds)) {
            countByDeck.put((Long) row[0], ((Number) row[1]).longValue());
        }

        return all.stream()
                .map(deck -> new DeckView(
                        deck.getId(),
                        deck.getName(),
                        deck.getSlug(),
                        deck.getDescription(),
                        deck.isSystem(),
                        isOwnedBy(deck, userId),
                        countByDeck.getOrDefault(deck.getId(), 0L).intValue(),
                        subscribed.contains(deck.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public DeckDetailView getDeck(Long userId, Long deckId) {
        DeckEntity deck = decks.findById(deckId)
                .orElseThrow(() -> new NoSuchElementException("Deck not found: " + deckId));
        ensureVisible(deck, userId);

        boolean subscribed = userDecks.existsById(new UserDeckId(userId, deckId));

        List<DeckHanziEntity> entries = deckHanzi.findByDeckIdOrderByPositionAsc(deckId);
        List<Long> hanziIds = entries.stream().map(e -> e.getHanzi().getId()).toList();
        Map<Long, List<String>> meaningsByHanzi = loadEnglishMeanings(hanziIds);

        List<DeckHanziView> entryViews = entries.stream()
                .map(e -> {
                    HanziEntity h = e.getHanzi();
                    return new DeckHanziView(
                            h.getId(),
                            h.getCharacter(),
                            h.getPinyin(),
                            h.getHskLevel(),
                            e.getPosition(),
                            meaningsByHanzi.getOrDefault(h.getId(), List.of()));
                })
                .toList();

        return new DeckDetailView(
                deck.getId(),
                deck.getName(),
                deck.getSlug(),
                deck.getDescription(),
                deck.isSystem(),
                isOwnedBy(deck, userId),
                subscribed,
                entries.size(),
                entryViews);
    }

    @Transactional
    public DeckView createDeck(Long userId, CreateDeckRequest request) {
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        String name = request.name().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Deck name must not be blank");
        }
        String slug = uniqueSlugForOwner(user.getId(), name);

        DeckEntity deck = decks.save(DeckEntity.builder()
                .name(name)
                .slug(slug)
                .system(false)
                .description(emptyToNull(request.description()))
                .owner(user)
                .build());

        return toView(deck, userId, false);
    }

    @Transactional
    public DeckView updateDeck(Long userId, Long deckId, UpdateDeckRequest request) {
        DeckEntity deck = decks.findById(deckId)
                .orElseThrow(() -> new NoSuchElementException("Deck not found: " + deckId));
        ensureOwned(deck, userId);

        String newName = request.name().trim();
        if (newName.isEmpty()) {
            throw new IllegalArgumentException("Deck name must not be blank");
        }
        if (!newName.equals(deck.getName())) {
            deck.setName(newName);
            deck.setSlug(uniqueSlugForOwner(userId, newName, deck.getId()));
        }
        deck.setDescription(emptyToNull(request.description()));
        DeckEntity saved = decks.save(deck);

        boolean subscribed = userDecks.existsById(new UserDeckId(userId, saved.getId()));
        return toView(saved, userId, subscribed);
    }

    @Transactional
    public void deleteDeck(Long userId, Long deckId) {
        DeckEntity deck = decks.findById(deckId)
                .orElseThrow(() -> new NoSuchElementException("Deck not found: " + deckId));
        ensureOwned(deck, userId);
        decks.delete(deck);
    }

    @Transactional
    public DeckDetailView addHanzi(Long userId, Long deckId, List<Long> hanziIds) {
        DeckEntity deck = decks.findById(deckId)
                .orElseThrow(() -> new NoSuchElementException("Deck not found: " + deckId));
        ensureOwned(deck, userId);

        // Distinct, non-null candidates that are not already in the deck.
        Set<Long> existing = deckHanzi.findHanziIdsByDeckId(deckId);
        java.util.LinkedHashSet<Long> toAdd = new java.util.LinkedHashSet<>();
        for (Long id : hanziIds) {
            if (id == null || existing.contains(id)) continue;
            toAdd.add(id);
        }

        if (!toAdd.isEmpty()) {
            // Batch-load hanzi entities, then batch-insert deck_hanzi rows.
            Map<Long, HanziEntity> hanziById = new HashMap<>();
            for (HanziEntity h : hanzi.findAllById(toAdd)) {
                hanziById.put(h.getId(), h);
            }
            int nextPos = deckHanzi.findMaxPositionByDeckId(deckId) + 1;
            List<DeckHanziEntity> rows = new ArrayList<>(toAdd.size());
            for (Long id : toAdd) {
                HanziEntity h = hanziById.get(id);
                if (h == null) throw new NoSuchElementException("Hanzi not found: " + id);
                rows.add(DeckHanziEntity.builder()
                        .id(new DeckHanziId(deckId, h.getId()))
                        .deck(deck)
                        .hanzi(h)
                        .position(nextPos++)
                        .build());
            }
            deckHanzi.saveAll(rows);

            if (userDecks.existsById(new UserDeckId(userId, deckId))) {
                UserEntity user = users.getReferenceById(userId);
                List<HanziEntity> publishedNew = rows.stream()
                        .map(DeckHanziEntity::getHanzi)
                        .filter(h -> h.getStatus() == HanziStatus.PUBLISHED)
                        .toList();
                bulkEnsureCards(user, publishedNew, Instant.now());
            }
        }

        return getDeck(userId, deckId);
    }

    @Transactional
    public void removeHanzi(Long userId, Long deckId, Long hanziId) {
        DeckEntity deck = decks.findById(deckId)
                .orElseThrow(() -> new NoSuchElementException("Deck not found: " + deckId));
        ensureOwned(deck, userId);
        deckHanzi.deleteByDeckIdAndHanziId(deckId, hanziId);
        // Single SQL statement repacks remaining positions to be contiguous.
        deckHanzi.repackPositions(deckId);
    }

    @Transactional
    public SubscribeResponse subscribe(Long userId, Long deckId) {
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        DeckEntity deck = decks.findById(deckId)
                .orElseThrow(() -> new NoSuchElementException("Deck not found: " + deckId));
        ensureVisible(deck, userId);

        UserDeckId udId = new UserDeckId(user.getId(), deck.getId());
        boolean already = userDecks.existsById(udId);
        if (!already) {
            userDecks.save(UserDeckEntity.builder()
                    .id(udId)
                    .user(user)
                    .deck(deck)
                    .subscribedAt(Instant.now())
                    .build());
        }

        List<DeckHanziEntity> entries = deckHanzi.findByDeckIdOrderByPositionAsc(deckId);
        List<HanziEntity> published = entries.stream()
                .map(DeckHanziEntity::getHanzi)
                .filter(h -> h.getStatus() == HanziStatus.PUBLISHED)
                .toList();
        int newlyCreated = bulkEnsureCards(user, published, Instant.now());
        return new SubscribeResponse(deck.getId(), newlyCreated, entries.size(), already);
    }

    /**
     * Creates RECOGNITION + PRODUCTION cards for any (hanzi, mode) pair the user does not yet
     * have. Performs a single SELECT to discover existing pairs and a single batched INSERT.
     */
    private int bulkEnsureCards(UserEntity user, List<HanziEntity> hanziList, Instant now) {
        if (hanziList.isEmpty()) return 0;
        List<Long> ids = hanziList.stream().map(HanziEntity::getId).toList();
        Set<String> existing = new HashSet<>();
        for (Object[] row : userCards.findExistingHanziModes(user.getId(), ids)) {
            existing.add(row[0] + ":" + ((StudyMode) row[1]).name());
        }
        List<UserCardEntity> toInsert = new ArrayList<>();
        for (HanziEntity h : hanziList) {
            for (StudyMode mode : new StudyMode[] {StudyMode.RECOGNITION, StudyMode.PRODUCTION}) {
                if (existing.contains(h.getId() + ":" + mode.name())) continue;
                toInsert.add(UserCardEntity.builder()
                        .user(user)
                        .hanzi(h)
                        .mode(mode)
                        .state(CardState.NEW)
                        .stability(0.0)
                        .difficulty(0.0)
                        .dueDate(now)
                        .reps(0)
                        .lapses(0)
                        .elapsedDays(0.0)
                        .scheduledDays(0.0)
                        .algorithmVersion("FSRS-5")
                        .build());
            }
        }
        if (toInsert.isEmpty()) return 0;
        userCards.saveAll(toInsert);
        return toInsert.size();
    }

    @Transactional
    public boolean unsubscribe(Long userId, Long deckId) {
        DeckEntity deck = decks.findById(deckId)
                .orElseThrow(() -> new NoSuchElementException("Deck not found: " + deckId));
        ensureVisible(deck, userId);

        UserDeckId udId = new UserDeckId(userId, deck.getId());
        if (!userDecks.existsById(udId)) {
            return false;
        }
        userDecks.deleteById(udId);
        return true;
    }

    private DeckView toView(DeckEntity deck, Long userId, boolean subscribed) {
        return new DeckView(
                deck.getId(),
                deck.getName(),
                deck.getSlug(),
                deck.getDescription(),
                deck.isSystem(),
                isOwnedBy(deck, userId),
                (int) deckHanzi.countByDeckId(deck.getId()),
                subscribed);
    }

    private boolean isOwnedBy(DeckEntity deck, Long userId) {
        return deck.getOwner() != null && userId != null && userId.equals(deck.getOwner().getId());
    }

    private void ensureVisible(DeckEntity deck, Long userId) {
        if (deck.isSystem()) return;
        if (deck.getOwner() == null || !deck.getOwner().getId().equals(userId)) {
            throw new NoSuchElementException("Deck not found: " + deck.getId());
        }
    }

    private void ensureOwned(DeckEntity deck, Long userId) {
        if (deck.getOwner() == null || !deck.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Deck " + deck.getId() + " is not owned by user " + userId);
        }
    }

    private String uniqueSlugForOwner(Long ownerId, String name) {
        return uniqueSlugForOwner(ownerId, name, null);
    }

    private String uniqueSlugForOwner(Long ownerId, String name, Long excludeDeckId) {
        String base = slugify(name);
        if (base.isEmpty()) base = "deck";

        // Single DB hit: load every slug that could collide. If the exclude id is set, we still
        // need to know whether the matching slug is the deck being renamed, so look it up once.
        Set<String> taken = new HashSet<>(decks.findSlugsByOwnerIdStartingWith(ownerId, base + "%"));
        if (excludeDeckId != null) {
            decks.findById(excludeDeckId).ifPresent(d -> taken.remove(d.getSlug()));
        }

        if (!taken.contains(base)) return base;
        for (int suffix = 2; suffix <= 1000; suffix++) {
            String candidate = base + "-" + suffix;
            if (!taken.contains(candidate)) return candidate;
        }
        throw new IllegalStateException("Could not allocate unique slug for deck name: " + name);
    }

    private static String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD);
        StringBuilder sb = new StringBuilder();
        boolean lastDash = false;
        for (int i = 0; i < normalized.length(); i++) {
            int cp = normalized.codePointAt(i);
            if (Character.isLetterOrDigit(cp)) {
                String piece = new String(Character.toChars(cp)).toLowerCase();
                sb.append(piece);
                lastDash = false;
            } else if (!lastDash && sb.length() > 0) {
                sb.append('-');
                lastDash = true;
            }
        }
        String out = sb.toString();
        while (out.endsWith("-")) out = out.substring(0, out.length() - 1);
        if (out.length() > 100) out = out.substring(0, 100);
        return out;
    }

    private static String emptyToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<Long, List<String>> loadEnglishMeanings(List<Long> hanziIds) {
        if (hanziIds.isEmpty()) return Collections.emptyMap();
        List<Object[]> rows = translations.findMeaningsByHanziIdInAndLanguage(hanziIds, "en");
        Map<Long, List<String>> out = new HashMap<>(rows.size());
        for (Object[] row : rows) {
            @SuppressWarnings("unchecked")
            List<String> meanings = (List<String>) row[1];
            out.put((Long) row[0], meanings);
        }
        return out;
    }

    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) {
            super(message);
        }
    }
}
