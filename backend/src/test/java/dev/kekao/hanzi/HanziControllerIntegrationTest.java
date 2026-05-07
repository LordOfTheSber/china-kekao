package dev.kekao.hanzi;

import dev.kekao.AbstractPostgresIntegrationTest;
import dev.kekao.auth.TokenService;
import dev.kekao.deck.DeckHanziRepository;
import dev.kekao.deck.DeckRepository;
import dev.kekao.deck.UserDeckRepository;
import dev.kekao.study.CardState;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIf(value = "dev.kekao.DockerAvailability#isAvailable",
        disabledReason = "Docker daemon is not available; integration tests skipped")
class HanziControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private HanziRepository hanzi;
    @Autowired private HanziTranslationRepository translations;
    @Autowired private HanziExampleRepository examples;
    @Autowired private UserRepository users;
    @Autowired private UserCardRepository userCards;
    @Autowired private ReviewLogRepository reviewLogs;
    @Autowired private DeckHanziRepository deckHanzi;
    @Autowired private UserDeckRepository userDecks;
    @Autowired private DeckRepository decks;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TokenService tokens;

    private String userToken;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        reviewLogs.deleteAll();
        userCards.deleteAll();
        userDecks.deleteAll();
        deckHanzi.deleteAll();
        decks.deleteAll();
        examples.deleteAll();
        translations.deleteAll();
        hanzi.deleteAll();
        users.deleteAll();

        user = users.save(UserEntity.builder()
                .email("hanzi-" + System.nanoTime() + "@e.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(UserRole.ROLE_USER)
                .settings(new HashMap<>())
                .build());
        userToken = tokens.issueAccessToken(user);
    }

    @Test
    void searchRequiresAuth() throws Exception {
        mvc.perform(get("/api/hanzi/search?q=ni"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void searchByPinyinReturnsPublishedOnly() throws Exception {
        seedPublished("你", "ni3", (short) 1, 1, List.of("you"));
        seedPublished("您", "nin2", (short) 3, 5000, List.of("you (polite)"));
        // Draft must be excluded:
        HanziEntity draft = hanzi.save(HanziEntity.builder()
                .character("妳").pinyin("ni3").hskLevel((short) 9)
                .status(HanziStatus.DRAFT).build());
        translations.save(HanziTranslationEntity.builder()
                .hanzi(draft).language("en")
                .meanings(new ArrayList<>(List.of("you (female)"))).primary(true).build());

        mvc.perform(get("/api/hanzi/search?q=ni").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].character").value("你"));
    }

    @Test
    void searchByMeaningMatchesEnglish() throws Exception {
        seedPublished("你", "ni3", (short) 1, 1, List.of("you"));
        seedPublished("好", "hao3", (short) 1, 2, List.of("good", "well"));

        mvc.perform(get("/api/hanzi/search?q=well")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].character").value("好"));
    }

    @Test
    void searchOrdersExactCharacterMatchFirst() throws Exception {
        seedPublished("你", "ni3", (short) 1, 5000, List.of("you"));
        seedPublished("妮", "ni1", (short) 4, 1, List.of("girl"));

        mvc.perform(get("/api/hanzi/search?q=你")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].character").value("你"));
    }

    @Test
    void searchFiltersByHskLevel() throws Exception {
        seedPublished("你", "ni3", (short) 1, 1, List.of("you"));
        seedPublished("您", "nin2", (short) 3, 2, List.of("you (polite)"));

        mvc.perform(get("/api/hanzi/search?q=&hsk=3")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].character").value("您"));
    }

    @Test
    void searchPaginates() throws Exception {
        for (int i = 0; i < 5; i++) {
            seedPublished("字" + i, "zi" + i, (short) 1, i + 1, List.of("char " + i));
        }
        mvc.perform(get("/api/hanzi/search?q=&page=1&size=2")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void detailReturns404ForUnknownId() throws Exception {
        mvc.perform(get("/api/hanzi/999999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void detailIncludesExamplesAndUserCardStates() throws Exception {
        HanziEntity ni = seedPublished("你", "nǐ", (short) 1, 1, List.of("you"));
        examples.save(HanziExampleEntity.builder()
                .hanzi(ni).language("en")
                .sentence("你好").pinyin("nǐ hǎo").translation("hello").build());
        userCards.save(UserCardEntity.builder()
                .user(user).hanzi(ni).mode(StudyMode.RECOGNITION).state(CardState.LEARNING)
                .stability(0).difficulty(0).dueDate(Instant.now())
                .reps(0).lapses(0).elapsedDays(0).scheduledDays(0)
                .algorithmVersion("FSRS-5").build());

        mvc.perform(get("/api/hanzi/" + ni.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.character").value("你"))
                .andExpect(jsonPath("$.meaningsEn[0]").value("you"))
                .andExpect(jsonPath("$.examples[0].sentence").value("你好"))
                .andExpect(jsonPath("$.userCards[0].mode").value("RECOGNITION"))
                .andExpect(jsonPath("$.userCards[0].state").value("LEARNING"));
    }

    private HanziEntity seedPublished(String character, String pinyin, short hsk, int freq,
                                      List<String> meanings) {
        HanziEntity h = hanzi.save(HanziEntity.builder()
                .character(character)
                .pinyin(pinyin)
                .hskLevel(hsk)
                .frequencyRank(freq)
                .status(HanziStatus.PUBLISHED)
                .build());
        translations.save(HanziTranslationEntity.builder()
                .hanzi(h)
                .language("en")
                .meanings(new ArrayList<>(meanings))
                .primary(true)
                .build());
        return h;
    }
}
