package dev.kekao.deck;

import dev.kekao.AbstractPostgresIntegrationTest;
import dev.kekao.auth.TokenService;
import dev.kekao.hanzi.HanziEntity;
import dev.kekao.hanzi.HanziRepository;
import dev.kekao.hanzi.HanziStatus;
import dev.kekao.study.StudyMode;
import dev.kekao.study.UserCardRepository;
import dev.kekao.user.UserEntity;
import dev.kekao.user.UserRepository;
import dev.kekao.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIf(value = "dev.kekao.DockerAvailability#isAvailable",
        disabledReason = "Docker daemon is not available; integration tests skipped")
class DeckControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private DeckRepository decks;
    @Autowired private DeckHanziRepository deckHanzi;
    @Autowired private UserDeckRepository userDecks;
    @Autowired private HanziRepository hanzi;
    @Autowired private UserRepository users;
    @Autowired private UserCardRepository userCards;
    @Autowired private DeckBootstrap deckBootstrap;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TokenService tokens;

    private UserEntity user;
    private String userToken;

    @BeforeEach
    void setUp() {
        userCards.deleteAll();
        userDecks.deleteAll();
        deckHanzi.deleteAll();
        decks.deleteAll();
        hanzi.deleteAll();
        users.deleteAll();

        user = users.save(UserEntity.builder()
                .email("decks-" + System.nanoTime() + "@e.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(UserRole.ROLE_USER)
                .settings(new HashMap<>())
                .build());
        userToken = tokens.issueAccessToken(user);
    }

    @AfterEach
    void cleanUp() {
        userCards.deleteAll();
        userDecks.deleteAll();
        deckHanzi.deleteAll();
        decks.deleteAll();
        hanzi.deleteAll();
        users.deleteAll();
    }

    @Test
    void listRequiresAuth() throws Exception {
        mvc.perform(get("/api/decks")).andExpect(status().isUnauthorized());
    }

    @Test
    void listExposesAllSeededHskDecks() throws Exception {
        deckBootstrap.ensureSystemDecks();

        mvc.perform(get("/api/decks").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decks.length()").value(DeckBootstrap.HSK_LEVELS.size()))
                .andExpect(jsonPath("$.decks[?(@.slug == 'hsk-1')].name", org.hamcrest.Matchers.contains("HSK 1")))
                .andExpect(jsonPath("$.decks[?(@.slug == 'hsk-2')].isSystem", org.hamcrest.Matchers.contains(true)));
    }

    @Test
    void hskBootstrapPopulatesDeckMembersFromPublishedHanzi() {
        seedPublished("你", "nǐ", (short) 1, 1);
        seedPublished("好", "hǎo", (short) 1, 2);
        seedPublished("二", "èr", (short) 2, 3);
        // Draft hanzi must be excluded.
        hanzi.save(HanziEntity.builder()
                .character("妳").pinyin("nǐ").hskLevel((short) 1)
                .status(HanziStatus.DRAFT).build());

        deckBootstrap.ensureSystemDecks();

        DeckEntity hsk1 = decks.findBySlug("hsk-1").orElseThrow();
        DeckEntity hsk2 = decks.findBySlug("hsk-2").orElseThrow();
        assertThat(deckHanzi.countByDeckId(hsk1.getId())).isEqualTo(2);
        assertThat(deckHanzi.countByDeckId(hsk2.getId())).isEqualTo(1);
    }

    @Test
    void hskBootstrapIsIdempotentAndResyncs() {
        HanziEntity ni = seedPublished("你", "nǐ", (short) 1, 1);
        deckBootstrap.ensureSystemDecks();
        DeckEntity hsk1 = decks.findBySlug("hsk-1").orElseThrow();
        long before = deckHanzi.countByDeckId(hsk1.getId());

        // Running again must not duplicate.
        deckBootstrap.ensureSystemDecks();
        assertThat(deckHanzi.countByDeckId(hsk1.getId())).isEqualTo(before);

        // Demoting a hanzi back to draft must remove it from the deck.
        ni.setStatus(HanziStatus.DRAFT);
        hanzi.save(ni);
        deckBootstrap.ensureSystemDecks();
        assertThat(deckHanzi.countByDeckId(hsk1.getId())).isZero();
    }

    @Test
    void subscribeCreatesUserCardsForBothModes() throws Exception {
        seedPublished("你", "nǐ", (short) 1, 1);
        seedPublished("好", "hǎo", (short) 1, 2);
        deckBootstrap.ensureSystemDecks();
        DeckEntity hsk1 = decks.findBySlug("hsk-1").orElseThrow();

        mvc.perform(post("/api/decks/" + hsk1.getId() + "/subscribe")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deckId").value(hsk1.getId()))
                .andExpect(jsonPath("$.newlyCreatedCards").value(4))
                .andExpect(jsonPath("$.alreadySubscribed").value(false));

        assertThat(userCards.findByUserIdAndHanziIdAndMode(
                user.getId(), hanzi.findByCharacter("你").orElseThrow().getId(), StudyMode.RECOGNITION))
                .isPresent();
        assertThat(userCards.findByUserIdAndHanziIdAndMode(
                user.getId(), hanzi.findByCharacter("好").orElseThrow().getId(), StudyMode.PRODUCTION))
                .isPresent();
    }

    @Test
    void subscribeIsIdempotent() throws Exception {
        seedPublished("你", "nǐ", (short) 1, 1);
        deckBootstrap.ensureSystemDecks();
        DeckEntity hsk1 = decks.findBySlug("hsk-1").orElseThrow();

        mvc.perform(post("/api/decks/" + hsk1.getId() + "/subscribe")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
        mvc.perform(post("/api/decks/" + hsk1.getId() + "/subscribe")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadySubscribed").value(true))
                .andExpect(jsonPath("$.newlyCreatedCards").value(0));
    }

    @Test
    void subscribeReturns404ForUnknownDeck() throws Exception {
        mvc.perform(post("/api/decks/999999/subscribe")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    private HanziEntity seedPublished(String character, String pinyin, short hsk, int freq) {
        return hanzi.save(HanziEntity.builder()
                .character(character)
                .pinyin(pinyin)
                .hskLevel(hsk)
                .frequencyRank(freq)
                .status(HanziStatus.PUBLISHED)
                .build());
    }
}
