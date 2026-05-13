package dev.kekao.study.session;

import dev.kekao.deck.DeckEntity;
import dev.kekao.deck.DeckRepository;
import dev.kekao.deck.UserDeckId;
import dev.kekao.deck.UserDeckRepository;
import dev.kekao.study.CardState;
import dev.kekao.study.UserCardEntity;
import dev.kekao.study.UserCardRepository;
import dev.kekao.user.UserEntity;
import dev.kekao.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * Builds today's study queue for a user (TASK-012):
 * <ol>
 *   <li>due review cards from subscribed decks (capped by {@code maxReviewsPerDay});</li>
 *   <li>new cards from subscribed decks (capped by {@code newPerDay});</li>
 *   <li>shuffled, then re-ordered so the two modes of the same hanzi are not adjacent.</li>
 * </ol>
 */
@Service
public class StudySessionService {

    public static final String SETTING_NEW_PER_DAY = "newPerDay";
    public static final String SETTING_MAX_REVIEWS_PER_DAY = "maxReviewsPerDay";
    public static final int DEFAULT_NEW_PER_DAY = 20;
    public static final int DEFAULT_MAX_REVIEWS_PER_DAY = 200;
    public static final int PRACTICE_QUEUE_MAX = 500;

    private final UserRepository users;
    private final UserCardRepository userCards;
    private final DeckRepository decks;
    private final UserDeckRepository userDecks;
    private final Clock clock;
    private final Random random;

    @Autowired
    public StudySessionService(UserRepository users,
                               UserCardRepository userCards,
                               DeckRepository decks,
                               UserDeckRepository userDecks) {
        this(users, userCards, decks, userDecks, Clock.systemUTC(), new Random());
    }

    StudySessionService(UserRepository users,
                        UserCardRepository userCards,
                        DeckRepository decks,
                        UserDeckRepository userDecks,
                        Clock clock,
                        Random random) {
        this.users = users;
        this.userCards = userCards;
        this.decks = decks;
        this.userDecks = userDecks;
        this.clock = clock;
        this.random = random;
    }

    @Transactional(readOnly = true)
    public List<UserCardEntity> getTodayQueue(Long userId) {
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        int newPerDay = readSetting(user.getSettings(), SETTING_NEW_PER_DAY, DEFAULT_NEW_PER_DAY);
        int maxReviewsPerDay = readSetting(user.getSettings(), SETTING_MAX_REVIEWS_PER_DAY, DEFAULT_MAX_REVIEWS_PER_DAY);

        Instant now = Instant.now(clock);
        List<UserCardEntity> reviews = maxReviewsPerDay <= 0
                ? List.of()
                : userCards.findDueReviewCardsForUser(userId, CardState.NEW, now,
                        PageRequest.of(0, maxReviewsPerDay));
        List<UserCardEntity> news = newPerDay <= 0
                ? List.of()
                : userCards.findCardsInStateForUser(userId, CardState.NEW,
                        PageRequest.of(0, newPerDay));

        List<UserCardEntity> combined = new ArrayList<>(reviews.size() + news.size());
        combined.addAll(reviews);
        combined.addAll(news);
        Collections.shuffle(combined, random);
        return separateSameHanzi(combined);
    }

    /**
     * On-demand queue for a single deck — returns every card the user has from that deck,
     * ignoring FSRS due dates and the daily new/review caps. Lets users replay a pack after
     * they've finished today's scheduled queue.
     */
    @Transactional(readOnly = true)
    public List<UserCardEntity> getDeckPracticeQueue(Long userId, Long deckId) {
        users.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        DeckEntity deck = decks.findById(deckId)
                .orElseThrow(() -> new NoSuchElementException("Deck not found: " + deckId));
        if (!deck.isSystem()
                && (deck.getOwner() == null || !userId.equals(deck.getOwner().getId()))
                && !userDecks.existsById(new UserDeckId(userId, deckId))) {
            throw new NoSuchElementException("Deck not found: " + deckId);
        }

        List<UserCardEntity> cards = new ArrayList<>(userCards.findAllCardsForUserAndDeck(
                userId, deckId, PageRequest.of(0, PRACTICE_QUEUE_MAX)));
        Collections.shuffle(cards, random);
        return separateSameHanzi(cards);
    }

    /**
     * Reorders the list so that no two consecutive cards reference the same hanzi.
     * Uses a one-pass swap: when a conflict is found, swap the offender with the next
     * non-conflicting card. Falls back gracefully if no swap is possible.
     */
    static List<UserCardEntity> separateSameHanzi(List<UserCardEntity> cards) {
        for (int i = 1; i < cards.size(); i++) {
            Long prev = cards.get(i - 1).getHanzi().getId();
            if (!prev.equals(cards.get(i).getHanzi().getId())) continue;
            int swapWith = -1;
            for (int j = i + 1; j < cards.size(); j++) {
                Long candidate = cards.get(j).getHanzi().getId();
                Long next = j + 1 < cards.size() ? cards.get(j + 1).getHanzi().getId() : null;
                if (!candidate.equals(prev) && (next == null || !next.equals(prev))) {
                    swapWith = j;
                    break;
                }
            }
            if (swapWith > 0) Collections.swap(cards, i, swapWith);
        }
        return cards;
    }

    private static int readSetting(Map<String, Object> settings, String key, int defaultValue) {
        if (settings == null) return defaultValue;
        Object raw = settings.get(key);
        if (raw instanceof Number n) return Math.max(0, n.intValue());
        if (raw instanceof String s) {
            try {
                return Math.max(0, Integer.parseInt(s.trim()));
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
