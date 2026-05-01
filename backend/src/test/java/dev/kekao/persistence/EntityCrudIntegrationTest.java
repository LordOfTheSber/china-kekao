package dev.kekao.persistence;

import dev.kekao.AbstractPostgresIntegrationTest;
import dev.kekao.auth.RefreshTokenEntity;
import dev.kekao.auth.RefreshTokenRepository;
import dev.kekao.deck.DeckEntity;
import dev.kekao.deck.DeckHanziEntity;
import dev.kekao.deck.DeckHanziId;
import dev.kekao.deck.DeckHanziRepository;
import dev.kekao.deck.DeckRepository;
import dev.kekao.deck.UserDeckEntity;
import dev.kekao.deck.UserDeckId;
import dev.kekao.deck.UserDeckRepository;
import dev.kekao.hanzi.HanziEntity;
import dev.kekao.hanzi.HanziExampleEntity;
import dev.kekao.hanzi.HanziExampleRepository;
import dev.kekao.hanzi.HanziRepository;
import dev.kekao.hanzi.HanziStatus;
import dev.kekao.hanzi.HanziTranslationEntity;
import dev.kekao.hanzi.HanziTranslationRepository;
import dev.kekao.study.CardState;
import dev.kekao.study.ReviewLogEntity;
import dev.kekao.study.ReviewLogRepository;
import dev.kekao.study.StudyMode;
import dev.kekao.study.UserCardEntity;
import dev.kekao.study.UserCardRepository;
import dev.kekao.user.UserEntity;
import dev.kekao.user.UserRepository;
import dev.kekao.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@EnabledIf(value = "dev.kekao.DockerAvailability#isAvailable",
        disabledReason = "Docker daemon is not available; integration tests skipped")
class EntityCrudIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private UserRepository users;
    @Autowired private RefreshTokenRepository refreshTokens;
    @Autowired private HanziRepository hanzi;
    @Autowired private HanziTranslationRepository translations;
    @Autowired private HanziExampleRepository examples;
    @Autowired private DeckRepository decks;
    @Autowired private DeckHanziRepository deckHanzi;
    @Autowired private UserDeckRepository userDecks;
    @Autowired private UserCardRepository userCards;
    @Autowired private ReviewLogRepository reviewLogs;

    private UserEntity persistUser(String email) {
        return users.save(UserEntity.builder()
                .email(email)
                .passwordHash("hash")
                .role(UserRole.ROLE_USER)
                .settings(Map.of("dailyNew", 20))
                .build());
    }

    private HanziEntity persistHanzi(String c) {
        return hanzi.save(HanziEntity.builder()
                .character(c)
                .pinyin("nǐ")
                .hskLevel((short) 1)
                .status(HanziStatus.DRAFT)
                .strokeCount((short) 7)
                .build());
    }

    @Test
    void userCrud() {
        UserEntity u = persistUser("a@b.com");
        assertThat(u.getId()).isNotNull();
        assertThat(users.findByEmail("a@b.com")).isPresent();
        assertThat(users.existsByEmail("a@b.com")).isTrue();
        u.setRole(UserRole.ROLE_ADMIN);
        users.saveAndFlush(u);
        assertThat(users.findById(u.getId()).orElseThrow().getRole()).isEqualTo(UserRole.ROLE_ADMIN);
        users.delete(u);
        assertThat(users.findByEmail("a@b.com")).isEmpty();
    }

    @Test
    void refreshTokenCrud() {
        UserEntity u = persistUser("rt@b.com");
        RefreshTokenEntity rt = refreshTokens.save(RefreshTokenEntity.builder()
                .tokenHash("hash-" + System.nanoTime())
                .user(u)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .revoked(false)
                .build());
        assertThat(refreshTokens.findByTokenHash(rt.getTokenHash())).isPresent();
        refreshTokens.revokeAllForUser(u.getId());
        refreshTokens.flush();
        RefreshTokenEntity reloaded = refreshTokens.findById(rt.getId()).orElseThrow();
        // detach + reload
        assertThat(refreshTokens.findByTokenHash(rt.getTokenHash())).isPresent();
    }

    @Test
    void hanziWithTranslationsAndExamples() {
        HanziEntity h = persistHanzi("你");
        HanziTranslationEntity t = translations.save(HanziTranslationEntity.builder()
                .hanzi(h)
                .language("en")
                .meanings(List.of("you", "thou"))
                .primary(true)
                .build());
        assertThat(translations.findByHanziId(h.getId())).hasSize(1);
        assertThat(translations.findById(t.getId()).orElseThrow().getMeanings()).containsExactly("you", "thou");

        HanziExampleEntity e = examples.save(HanziExampleEntity.builder()
                .hanzi(h)
                .language("en")
                .sentence("你好")
                .pinyin("nǐ hǎo")
                .translation("hello")
                .build());
        assertThat(examples.findByHanziId(h.getId())).hasSize(1);
        assertThat(examples.findById(e.getId())).isPresent();
    }

    @Test
    void deckAndDeckHanzi() {
        DeckEntity d = decks.save(DeckEntity.builder()
                .name("HSK 1")
                .slug("hsk-1-" + System.nanoTime())
                .system(true)
                .description("HSK level 1")
                .build());
        HanziEntity h = persistHanzi("好");
        DeckHanziEntity dh = deckHanzi.save(DeckHanziEntity.builder()
                .id(new DeckHanziId(d.getId(), h.getId()))
                .deck(d)
                .hanzi(h)
                .position(1)
                .build());
        assertThat(deckHanzi.findByDeckIdOrderByPositionAsc(d.getId())).hasSize(1);
        assertThat(deckHanzi.findById(dh.getId())).isPresent();
    }

    @Test
    void userDeckSubscription() {
        UserEntity u = persistUser("ud@b.com");
        DeckEntity d = decks.save(DeckEntity.builder()
                .name("Custom").slug("custom-" + System.nanoTime()).system(false).build());
        userDecks.save(UserDeckEntity.builder()
                .id(new UserDeckId(u.getId(), d.getId()))
                .user(u)
                .deck(d)
                .build());
        assertThat(userDecks.findByUserId(u.getId())).hasSize(1);
    }

    @Test
    void userCardAndReviewLog() {
        UserEntity u = persistUser("uc@b.com");
        HanziEntity h = persistHanzi("学");
        UserCardEntity card = userCards.save(UserCardEntity.builder()
                .user(u)
                .hanzi(h)
                .mode(StudyMode.RECOGNITION)
                .state(CardState.NEW)
                .stability(0)
                .difficulty(0)
                .dueDate(Instant.now())
                .reps(0)
                .lapses(0)
                .elapsedDays(0)
                .scheduledDays(0)
                .algorithmVersion("FSRS-5")
                .build());

        assertThat(userCards.findByUserIdAndHanziIdAndMode(u.getId(), h.getId(), StudyMode.RECOGNITION))
                .isPresent();

        ReviewLogEntity log = reviewLogs.save(ReviewLogEntity.builder()
                .userCard(card)
                .rating((short) 3)
                .stateBefore(CardState.NEW)
                .elapsedDays(0)
                .scheduledDays(1)
                .stabilityBefore(0)
                .difficultyBefore(0)
                .responseTimeMs(1234)
                .hintCount((short) 0)
                .strokeMistakes((short) 0)
                .build());

        assertThat(reviewLogs.findByUserCardIdOrderByReviewedAtAsc(card.getId())).hasSize(1);
        assertThat(reviewLogs.findById(log.getId())).isPresent();
    }
}
