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

/**
 * Round-trip CRUD checks for every JPA entity introduced in TASK-004.
 * Exercising real PostgreSQL via Testcontainers gives us coverage for
 * {@code text[]} and {@code jsonb} mappings that an in-memory database
 * cannot validate.
 */
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

    @Test
    void userCrud() {
        UserEntity created = persistUser("a@b.com");

        assertThat(created.getId()).isNotNull();
        assertThat(users.findByEmail("a@b.com")).isPresent();
        assertThat(users.existsByEmail("a@b.com")).isTrue();

        created.setRole(UserRole.ROLE_ADMIN);
        users.saveAndFlush(created);
        assertThat(users.findById(created.getId()).orElseThrow().getRole())
                .isEqualTo(UserRole.ROLE_ADMIN);

        users.delete(created);
        assertThat(users.findByEmail("a@b.com")).isEmpty();
    }

    @Test
    void refreshTokenCrud() {
        UserEntity owner = persistUser("rt@b.com");
        RefreshTokenEntity stored = refreshTokens.save(buildRefreshToken(owner, "hash-rt"));

        assertThat(refreshTokens.findByTokenHash("hash-rt"))
                .map(RefreshTokenEntity::getId)
                .contains(stored.getId());

        int revokedCount = refreshTokens.revokeAllForUser(owner.getId());

        assertThat(revokedCount).isEqualTo(1);
        assertThat(refreshTokens.findByTokenHash("hash-rt")
                .orElseThrow().isRevoked()).isTrue();
    }

    @Test
    void hanziWithTranslationsAndExamples() {
        HanziEntity character = persistHanzi("你");
        translations.save(HanziTranslationEntity.builder()
                .hanzi(character).language("en")
                .meanings(List.of("you", "thou")).primary(true).build());
        examples.save(HanziExampleEntity.builder()
                .hanzi(character).language("en")
                .sentence("你好").pinyin("nǐ hǎo").translation("hello").build());

        List<HanziTranslationEntity> savedTranslations =
                translations.findByHanziId(character.getId());
        assertThat(savedTranslations).singleElement()
                .extracting(HanziTranslationEntity::getMeanings)
                .isEqualTo(List.of("you", "thou"));
        assertThat(examples.findByHanziId(character.getId())).hasSize(1);
    }

    @Test
    void deckAndDeckHanzi() {
        DeckEntity deck = decks.save(buildDeck("hsk-1", true));
        HanziEntity character = persistHanzi("好");
        DeckHanziId membershipId = new DeckHanziId(deck.getId(), character.getId());

        deckHanzi.save(DeckHanziEntity.builder()
                .id(membershipId).deck(deck).hanzi(character).position(1).build());

        assertThat(deckHanzi.findById(membershipId)).isPresent();
        assertThat(deckHanzi.findByDeckIdOrderByPositionAsc(deck.getId()))
                .singleElement()
                .extracting(DeckHanziEntity::getPosition).isEqualTo(1);
    }

    @Test
    void userDeckSubscription() {
        UserEntity subscriber = persistUser("ud@b.com");
        DeckEntity deck = decks.save(buildDeck("custom", false));

        userDecks.save(UserDeckEntity.builder()
                .id(new UserDeckId(subscriber.getId(), deck.getId()))
                .user(subscriber).deck(deck).build());

        assertThat(userDecks.findByUserId(subscriber.getId())).hasSize(1);
    }

    @Test
    void userCardAndReviewLog() {
        UserEntity owner = persistUser("uc@b.com");
        HanziEntity character = persistHanzi("学");
        UserCardEntity card = userCards.save(buildUserCard(owner, character));

        assertThat(userCards.findByUserIdAndHanziIdAndMode(
                owner.getId(), character.getId(), StudyMode.RECOGNITION))
                .map(UserCardEntity::getId)
                .contains(card.getId());

        reviewLogs.save(buildReviewLog(card));

        assertThat(reviewLogs.findByUserCardIdOrderByReviewedAtAsc(card.getId()))
                .hasSize(1);
    }

    private UserEntity persistUser(String email) {
        return users.save(UserEntity.builder()
                .email(email)
                .passwordHash("hash")
                .role(UserRole.ROLE_USER)
                .settings(Map.of("dailyNew", 20))
                .build());
    }

    private HanziEntity persistHanzi(String character) {
        return hanzi.save(HanziEntity.builder()
                .character(character)
                .pinyin("nǐ")
                .hskLevel((short) 1)
                .strokeCount((short) 7)
                .status(HanziStatus.DRAFT)
                .build());
    }

    private RefreshTokenEntity buildRefreshToken(UserEntity owner, String tokenHash) {
        return RefreshTokenEntity.builder()
                .tokenHash(tokenHash)
                .user(owner)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .revoked(false)
                .build();
    }

    private DeckEntity buildDeck(String slugPrefix, boolean system) {
        return DeckEntity.builder()
                .name(slugPrefix.toUpperCase())
                .slug(slugPrefix + "-" + System.nanoTime())
                .system(system)
                .description("test deck")
                .build();
    }

    private UserCardEntity buildUserCard(UserEntity owner, HanziEntity character) {
        return UserCardEntity.builder()
                .user(owner).hanzi(character)
                .mode(StudyMode.RECOGNITION).state(CardState.NEW)
                .stability(0).difficulty(0)
                .dueDate(Instant.now())
                .reps(0).lapses(0)
                .elapsedDays(0).scheduledDays(0)
                .algorithmVersion("FSRS-5")
                .build();
    }

    private ReviewLogEntity buildReviewLog(UserCardEntity card) {
        return ReviewLogEntity.builder()
                .userCard(card)
                .user(card.getUser())
                .rating((short) 3)
                .stateBefore(CardState.NEW)
                .elapsedDays(0).scheduledDays(1)
                .stabilityBefore(0).difficultyBefore(0)
                .responseTimeMs(1234)
                .hintCount((short) 0).strokeMistakes((short) 0)
                .build();
    }
}
