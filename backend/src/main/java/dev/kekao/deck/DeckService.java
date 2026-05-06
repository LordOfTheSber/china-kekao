package dev.kekao.deck;

import dev.kekao.deck.DeckDtos.DeckView;
import dev.kekao.deck.DeckDtos.SubscribeResponse;
import dev.kekao.hanzi.HanziEntity;
import dev.kekao.hanzi.HanziRepository;
import dev.kekao.hanzi.HanziStatus;
import dev.kekao.study.CardState;
import dev.kekao.study.StudyMode;
import dev.kekao.study.UserCardEntity;
import dev.kekao.study.UserCardRepository;
import dev.kekao.user.UserEntity;
import dev.kekao.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
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

    @Autowired
    public DeckService(DeckRepository decks,
                       DeckHanziRepository deckHanzi,
                       UserDeckRepository userDecks,
                       UserCardRepository userCards,
                       UserRepository users,
                       HanziRepository hanzi) {
        this.decks = decks;
        this.deckHanzi = deckHanzi;
        this.userDecks = userDecks;
        this.userCards = userCards;
        this.users = users;
        this.hanzi = hanzi;
    }

    @Transactional(readOnly = true)
    public List<DeckView> listSystemDecks(Long userId) {
        Set<Long> subscribed = userDecks.findByUserId(userId).stream()
                .map(ud -> ud.getDeck().getId())
                .collect(java.util.stream.Collectors.toSet());
        return decks.findAllBySystemTrueOrderByIdAsc().stream()
                .map(deck -> new DeckView(
                        deck.getId(),
                        deck.getName(),
                        deck.getSlug(),
                        deck.getDescription(),
                        deck.isSystem(),
                        (int) deckHanzi.countByDeckId(deck.getId()),
                        subscribed.contains(deck.getId())))
                .toList();
    }

    @Transactional
    public SubscribeResponse subscribe(Long userId, Long deckId) {
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        DeckEntity deck = decks.findById(deckId)
                .orElseThrow(() -> new NoSuchElementException("Deck not found: " + deckId));

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
}
