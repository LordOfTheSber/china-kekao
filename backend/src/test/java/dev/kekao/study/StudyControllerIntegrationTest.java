package dev.kekao.study;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import dev.kekao.hanzi.HanziTranslationEntity;
import dev.kekao.hanzi.HanziTranslationRepository;
import dev.kekao.user.UserEntity;
import dev.kekao.user.UserRepository;
import dev.kekao.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIf(value = "dev.kekao.DockerAvailability#isAvailable",
        disabledReason = "Docker daemon is not available; integration tests skipped")
class StudyControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UserRepository users;
    @Autowired private HanziRepository hanzi;
    @Autowired private HanziTranslationRepository translations;
    @Autowired private DeckRepository decks;
    @Autowired private DeckHanziRepository deckHanzi;
    @Autowired private UserDeckRepository userDecks;
    @Autowired private UserCardRepository userCards;
    @Autowired private ReviewLogRepository reviewLogs;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TokenService tokens;

    private UserEntity owner;
    private UserEntity stranger;
    private String ownerToken;
    private String strangerToken;

    @BeforeEach
    void setUp() {
        reviewLogs.deleteAll();
        userCards.deleteAll();
        userDecks.deleteAll();
        deckHanzi.deleteAll();
        decks.deleteAll();
        translations.deleteAll();
        hanzi.deleteAll();
        users.deleteAll();

        owner = createUser("owner-" + System.nanoTime() + "@e.com");
        stranger = createUser("stranger-" + System.nanoTime() + "@e.com");
        ownerToken = tokens.issueAccessToken(owner);
        strangerToken = tokens.issueAccessToken(stranger);
    }

    private UserEntity createUser(String email) {
        return users.save(UserEntity.builder()
                .email(email.toLowerCase())
                .passwordHash(passwordEncoder.encode("password123"))
                .role(UserRole.ROLE_USER)
                .settings(new HashMap<>())
                .build());
    }

    private HanziEntity seedHanzi(String character, String pinyin, List<String> meanings) {
        HanziEntity h = hanzi.save(HanziEntity.builder()
                .character(character)
                .pinyin(pinyin)
                .status(HanziStatus.PUBLISHED)
                .build());
        translations.save(HanziTranslationEntity.builder()
                .hanzi(h)
                .language("en")
                .meanings(new java.util.ArrayList<>(meanings))
                .primary(true)
                .build());
        return h;
    }

    private DeckEntity seedDeck(UserEntity subscriber, HanziEntity... entries) {
        DeckEntity deck = decks.save(DeckEntity.builder()
                .name("Test deck")
                .slug("test-" + System.nanoTime())
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
        return deck;
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

    @Test
    void sessionRequiresAuth() throws Exception {
        mvc.perform(get("/api/study/session"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sessionReturnsRecognitionAndProductionPayloads() throws Exception {
        HanziEntity ni = seedHanzi("你", "nǐ", List.of("you"));
        HanziEntity hao = seedHanzi("好", "hǎo", List.of("good"));
        seedDeck(owner, ni, hao);

        Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
        seedCard(owner, ni, StudyMode.RECOGNITION, CardState.REVIEW, past);
        seedCard(owner, hao, StudyMode.PRODUCTION, CardState.REVIEW, past);

        mvc.perform(get("/api/study/session")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.mode == 'RECOGNITION')].character",
                        org.hamcrest.Matchers.contains("你")))
                .andExpect(jsonPath("$[?(@.mode == 'RECOGNITION')].meanings[0]",
                        org.hamcrest.Matchers.contains("you")))
                .andExpect(jsonPath("$[?(@.mode == 'PRODUCTION')].character[0]")
                        .doesNotExist())
                .andExpect(jsonPath("$[?(@.mode == 'PRODUCTION')].meanings[0]",
                        org.hamcrest.Matchers.contains("good")));
    }

    @Test
    void sessionExcludesOtherUsersCards() throws Exception {
        HanziEntity ni = seedHanzi("你", "nǐ", List.of("you"));
        seedDeck(stranger, ni);
        seedCard(stranger, ni, StudyMode.RECOGNITION, CardState.REVIEW,
                Instant.now().minus(1, ChronoUnit.DAYS));

        mvc.perform(get("/api/study/session")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void reviewHappyPathUpdatesCard() throws Exception {
        HanziEntity ni = seedHanzi("你", "nǐ", List.of("you"));
        seedDeck(owner, ni);
        UserCardEntity card = seedCard(owner, ni, StudyMode.RECOGNITION,
                CardState.NEW, Instant.now().minus(1, ChronoUnit.DAYS));

        mvc.perform(post("/api/study/review")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "userCardId", card.getId(),
                                "mode", "RECOGNITION",
                                "rating", "GOOD",
                                "responseTimeMs", 1500))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCardId").value(card.getId()))
                .andExpect(jsonPath("$.nextDue").exists())
                .andExpect(jsonPath("$.state").exists());

        UserCardEntity reloaded = userCards.findById(card.getId()).orElseThrow();
        assertThat(reloaded.getReps()).isEqualTo(1);
        assertThat(reloaded.getLastReview()).isNotNull();
        assertThat(reviewLogs.findByUserCardIdOrderByReviewedAtAsc(card.getId())).hasSize(1);
    }

    @Test
    void reviewWithoutTokenIsUnauthorized() throws Exception {
        mvc.perform(post("/api/study/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "userCardId", 1,
                                "mode", "RECOGNITION",
                                "rating", "GOOD"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reviewForCardOwnedByAnotherUserIsForbidden() throws Exception {
        HanziEntity ni = seedHanzi("你", "nǐ", List.of("you"));
        seedDeck(stranger, ni);
        UserCardEntity card = seedCard(stranger, ni, StudyMode.RECOGNITION,
                CardState.NEW, Instant.now().minus(1, ChronoUnit.DAYS));

        mvc.perform(post("/api/study/review")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "userCardId", card.getId(),
                                "mode", "RECOGNITION",
                                "rating", "GOOD"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("user_card_forbidden"));
    }

    @Test
    void reviewWithUnknownCardReturns404() throws Exception {
        mvc.perform(post("/api/study/review")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "userCardId", 9_999_999L,
                                "mode", "RECOGNITION",
                                "rating", "GOOD"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("user_card_not_found"));
    }

    @Test
    void reviewWithModeMismatchIs422() throws Exception {
        HanziEntity ni = seedHanzi("你", "nǐ", List.of("you"));
        seedDeck(owner, ni);
        UserCardEntity card = seedCard(owner, ni, StudyMode.RECOGNITION,
                CardState.NEW, Instant.now().minus(1, ChronoUnit.DAYS));

        mvc.perform(post("/api/study/review")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "userCardId", card.getId(),
                                "mode", "PRODUCTION",
                                "rating", "GOOD"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("mode_mismatch"));
    }
}
