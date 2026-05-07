package dev.kekao.stats;

import dev.kekao.AbstractPostgresIntegrationTest;
import dev.kekao.auth.TokenService;
import dev.kekao.deck.DeckEntity;
import dev.kekao.deck.DeckHanziEntity;
import dev.kekao.deck.DeckHanziId;
import dev.kekao.deck.DeckHanziRepository;
import dev.kekao.deck.DeckRepository;
import dev.kekao.deck.UserDeckEntity;
import dev.kekao.deck.UserDeckId;
import dev.kekao.deck.UserDeckRepository;
import dev.kekao.hanzi.HanziEntity;
import dev.kekao.hanzi.HanziRepository;
import dev.kekao.hanzi.HanziStatus;
import dev.kekao.study.CardState;
import dev.kekao.study.ReviewLogEntity;
import dev.kekao.study.ReviewLogRepository;
import dev.kekao.study.StudyMode;
import dev.kekao.study.UserCardEntity;
import dev.kekao.study.UserCardRepository;
import dev.kekao.user.UserEntity;
import dev.kekao.user.UserRepository;
import dev.kekao.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIf(value = "dev.kekao.DockerAvailability#isAvailable",
        disabledReason = "Docker daemon is not available; integration tests skipped")
class StatsControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private HanziRepository hanzi;
    @Autowired private DeckRepository decks;
    @Autowired private DeckHanziRepository deckHanzi;
    @Autowired private UserDeckRepository userDecks;
    @Autowired private UserCardRepository userCards;
    @Autowired private ReviewLogRepository reviewLogs;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TokenService tokens;

    private UserEntity owner;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        reviewLogs.deleteAll();
        userCards.deleteAll();
        userDecks.deleteAll();
        deckHanzi.deleteAll();
        decks.deleteAll();
        hanzi.deleteAll();
        users.deleteAll();

        owner = users.save(UserEntity.builder()
                .email("stats-" + System.nanoTime() + "@e.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(UserRole.ROLE_USER)
                .settings(new HashMap<>())
                .build());
        ownerToken = tokens.issueAccessToken(owner);
    }

    @Test
    void dashboardRequiresAuth() throws Exception {
        mvc.perform(get("/api/stats/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardReportsExpectedCounts() throws Exception {
        HanziEntity h1 = seedHanzi("一", "yī");
        HanziEntity h2 = seedHanzi("二", "èr");
        HanziEntity h3 = seedHanzi("三", "sān");
        seedDeck(owner, h1, h2, h3);

        Instant now = Instant.now();
        UserCardEntity due = seedCard(owner, h1, StudyMode.RECOGNITION,
                CardState.REVIEW, now.minus(1, ChronoUnit.DAYS));
        seedCard(owner, h2, StudyMode.RECOGNITION, CardState.NEW, now);
        seedCard(owner, h3, StudyMode.RECOGNITION, CardState.REVIEW, now.plus(5, ChronoUnit.DAYS));

        // 4 reviews in last 7 days: 3 GOOD, 1 AGAIN (=> 75% accuracy)
        for (int i = 0; i < 3; i++) {
            saveReviewLog(due, (short) 3, now.minus(i, ChronoUnit.DAYS));
        }
        saveReviewLog(due, (short) 1, now.minus(3, ChronoUnit.DAYS));

        mvc.perform(get("/api/stats/dashboard")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueTodayCount").value(1))
                .andExpect(jsonPath("$.newAvailableCount").value(1))
                .andExpect(jsonPath("$.learnedTotal").value(2))
                .andExpect(jsonPath("$.currentStreak").value(4))
                .andExpect(jsonPath("$.accuracy7d").value(0.75));
    }

    @Test
    void dashboardWithNoDataIsAllZeros() throws Exception {
        mvc.perform(get("/api/stats/dashboard")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueTodayCount").value(0))
                .andExpect(jsonPath("$.newAvailableCount").value(0))
                .andExpect(jsonPath("$.learnedTotal").value(0))
                .andExpect(jsonPath("$.currentStreak").value(0))
                .andExpect(jsonPath("$.accuracy7d").value(0.0));
    }

    @Test
    void overviewRequiresAuth() throws Exception {
        mvc.perform(get("/api/stats/overview")).andExpect(status().isUnauthorized());
    }

    @Test
    void overviewExposesDailyTotalsAndStateBreakdown() throws Exception {
        HanziEntity h1 = seedHanzi("一", "yī");
        HanziEntity h2 = seedHanzi("二", "èr");
        seedDeck(owner, h1, h2);

        Instant now = Instant.now();
        UserCardEntity reviewCard = seedCard(owner, h1, StudyMode.RECOGNITION,
                CardState.REVIEW, now);
        seedCard(owner, h2, StudyMode.RECOGNITION, CardState.NEW, now);

        // 2 GOOD today, 1 AGAIN yesterday.
        saveReviewLog(reviewCard, (short) 3, now);
        saveReviewLog(reviewCard, (short) 4, now);
        saveReviewLog(reviewCard, (short) 1, now.minus(1, ChronoUnit.DAYS));

        mvc.perform(get("/api/stats/overview?days=7")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").value(7))
                .andExpect(jsonPath("$.daily.length()").value(7))
                .andExpect(jsonPath("$.daily[6].total").value(2))
                .andExpect(jsonPath("$.daily[6].good").value(2))
                .andExpect(jsonPath("$.daily[6].accuracy").value(1.0))
                .andExpect(jsonPath("$.daily[5].total").value(1))
                .andExpect(jsonPath("$.daily[5].good").value(0))
                .andExpect(jsonPath("$.states.newCount").value(1))
                .andExpect(jsonPath("$.states.review").value(1));
    }

    @Test
    void overviewClampsDaysParameter() throws Exception {
        mvc.perform(get("/api/stats/overview?days=0")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").value(1));
        mvc.perform(get("/api/stats/overview?days=10000")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").value(365));
    }

    private HanziEntity seedHanzi(String character, String pinyin) {
        return hanzi.save(HanziEntity.builder()
                .character(character)
                .pinyin(pinyin)
                .status(HanziStatus.PUBLISHED)
                .build());
    }

    private void seedDeck(UserEntity subscriber, HanziEntity... entries) {
        DeckEntity deck = decks.save(DeckEntity.builder()
                .name("Stats deck")
                .slug("stats-" + System.nanoTime())
                .system(false)
                .build());
        int pos = 0;
        for (HanziEntity h : entries) {
            deckHanzi.save(DeckHanziEntity.builder()
                    .id(new DeckHanziId(deck.getId(), h.getId()))
                    .deck(deck)
                    .hanzi(h)
                    .position(pos++)
                    .build());
        }
        userDecks.save(UserDeckEntity.builder()
                .id(new UserDeckId(subscriber.getId(), deck.getId()))
                .user(subscriber)
                .deck(deck)
                .build());
    }

    private UserCardEntity seedCard(UserEntity user, HanziEntity h, StudyMode mode,
                                    CardState state, Instant due) {
        return userCards.save(UserCardEntity.builder()
                .user(user)
                .hanzi(h)
                .mode(mode)
                .state(state)
                .stability(0.5)
                .difficulty(5.0)
                .dueDate(due)
                .reps(0)
                .lapses(0)
                .elapsedDays(0)
                .scheduledDays(0)
                .algorithmVersion("FSRS-5")
                .build());
    }

    private void saveReviewLog(UserCardEntity card, short rating, Instant when) {
        reviewLogs.save(ReviewLogEntity.builder()
                .userCard(card)
                .rating(rating)
                .stateBefore(CardState.REVIEW)
                .elapsedDays(0)
                .scheduledDays(0)
                .stabilityBefore(0.5)
                .difficultyBefore(5.0)
                .reviewedAt(when)
                .responseTimeMs(1000)
                .hintCount((short) 0)
                .strokeMistakes((short) 0)
                .build());
    }
}
