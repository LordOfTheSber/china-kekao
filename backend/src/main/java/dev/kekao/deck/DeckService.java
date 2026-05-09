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
import dev.kekao.hanzi.HanziTranslationEntity;
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
import java.util.stream.Collectors;

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
        Set<Long> subscribed = userDecks.findByUserId(userId).stream()
                .map(ud -> ud.getDeck().getId())
                .collect(Collectors.toSet());

        List<DeckEntity> all = new ArrayList<>(decks.findAllBySystemTrueOrderByIdAsc());
        all.addAll(decks.findAllByOwnerIdOrderByIdAsc(userId));

        return all.stream()
                .map(deck -> toView(deck, userId, subscribed.contains(deck.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public DeckDetailView getDeck(Long userId, Long deckId) {
        DeckEntity deck = decks.findById(deckId)
                .orElseThrow(() -> new NoSuchElementException("Deck not found: " + deckId));
        ensureVisible(deck, userId);

        Set<Long> subscribed = userDecks.findByUserId(userId).stream()
                .map(ud -> ud.getDeck().getId())
                .collect(Collectors.toSet());

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
                subscribed.contains(deck.getId()),
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

        int nextPos = deckHanzi.findMaxPositionByDeckId(deckId) + 1;
        Set<Long> seen = new HashSet<>();
        for (Long id : hanziIds) {
            if (id == null || !seen.add(id)) continue;
            if (deckHanzi.findByDeckIdAndHanziId(deckId, id).isPresent()) continue;
            HanziEntity h = hanzi.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Hanzi not found: " + id));
            deckHanzi.save(DeckHanziEntity.builder()
                    .id(new DeckHanziId(deckId, h.getId()))
                    .deck(deck)
                    .hanzi(h)
                    .position(nextPos++)
                    .build());
        }

        // If the user is already subscribed to this deck, also create user_cards
        // for the newly added hanzi so the additions show up in the study queue.
        if (userDecks.existsById(new UserDeckId(userId, deckId))) {
            UserEntity user = users.getReferenceById(userId);
            Instant now = Instant.now();
            for (DeckHanziEntity e : deckHanzi.findByDeckIdOrderByPositionAsc(deckId)) {
                HanziEntity h = e.getHanzi();
                if (h.getStatus() != HanziStatus.PUBLISHED) continue;
                ensureCard(user, h, StudyMode.RECOGNITION, now);
                ensureCard(user, h, StudyMode.PRODUCTION, now);
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

        // Re-pack positions to keep them contiguous.
        List<DeckHanziEntity> remaining = deckHanzi.findByDeckIdOrderByPositionAsc(deckId);
        for (int i = 0; i < remaining.size(); i++) {
            DeckHanziEntity entry = remaining.get(i);
            if (entry.getPosition() == null || entry.getPosition() != i) {
                entry.setPosition(i);
                deckHanzi.save(entry);
            }
        }
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
        int newlyCreated = 0;
        Instant now = Instant.now();
        for (DeckHanziEntity entry : entries) {
            HanziEntity h = entry.getHanzi();
            if (h.getStatus() != HanziStatus.PUBLISHED) continue;
            newlyCreated += ensureCard(user, h, StudyMode.RECOGNITION, now);
            newlyCreated += ensureCard(user, h, StudyMode.PRODUCTION, now);
        }
        return new SubscribeResponse(deck.getId(), newlyCreated, entries.size(), already);
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

    private int ensureCard(UserEntity user, HanziEntity hanzi, StudyMode mode, Instant now) {
        if (userCards.findByUserIdAndHanziIdAndMode(user.getId(), hanzi.getId(), mode).isPresent()) {
            return 0;
        }
        userCards.save(UserCardEntity.builder()
                .user(user)
                .hanzi(hanzi)
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
        return 1;
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
        String candidate = base;
        int suffix = 2;
        while (true) {
            var existing = decks.findBySlugAndOwnerId(candidate, ownerId);
            if (existing.isEmpty() || (excludeDeckId != null && existing.get().getId().equals(excludeDeckId))) {
                return candidate;
            }
            candidate = base + "-" + suffix++;
            if (suffix > 1000) {
                throw new IllegalStateException("Could not allocate unique slug for deck name: " + name);
            }
        }
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
        Map<Long, List<String>> out = new HashMap<>();
        for (HanziTranslationEntity t : translations.findByHanziIdInAndLanguage(hanziIds, "en")) {
            out.put(t.getHanzi().getId(), t.getMeanings());
        }
        return out;
    }

    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) {
            super(message);
        }
    }
}
