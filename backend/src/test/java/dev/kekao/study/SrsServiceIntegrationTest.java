package dev.kekao.study;

import dev.kekao.AbstractPostgresIntegrationTest;
import dev.kekao.hanzi.HanziEntity;
import dev.kekao.hanzi.HanziRepository;
import dev.kekao.hanzi.HanziStatus;
import dev.kekao.user.UserEntity;
import dev.kekao.user.UserRepository;
import dev.kekao.user.UserRole;
import dev.kekao.study.srs.SrsScheduleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@EnabledIf(value = "dev.kekao.DockerAvailability#isAvailable",
        disabledReason = "Docker daemon is not available; integration tests skipped")
class SrsServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private SrsService srsService;
    @Autowired private UserRepository users;
    @Autowired private HanziRepository hanzi;
    @Autowired private UserCardRepository userCards;
    @Autowired private ReviewLogRepository reviewLogs;

    @BeforeEach
    void setUp() {
        reviewLogs.deleteAll();
        userCards.deleteAll();
        hanzi.deleteAll();
        users.deleteAll();
    }

    @Test
    void reviewUpdatesCardAndWritesLog() {
        UserEntity user = users.save(UserEntity.builder()
                .email("srs-" + System.nanoTime() + "@kekao.dev")
                .passwordHash("hash")
                .role(UserRole.ROLE_USER)
                .settings(Map.of())
                .build());
        HanziEntity symbol = hanzi.save(HanziEntity.builder()
                .character("你")
                .pinyin("nǐ")
                .status(HanziStatus.PUBLISHED)
                .build());
        Instant dueDate = Instant.now().minus(1, ChronoUnit.DAYS);
        UserCardEntity card = userCards.save(UserCardEntity.builder()
                .user(user)
                .hanzi(symbol)
                .mode(StudyMode.RECOGNITION)
                .state(CardState.NEW)
                .stability(0.5)
                .difficulty(5.0)
                .dueDate(dueDate)
                .reps(0)
                .lapses(0)
                .elapsedDays(0)
                .scheduledDays(0)
                .algorithmVersion("FSRS-5")
                .build());

        SrsScheduleResult result = srsService.review(
                card.getId(),
                Rating.GOOD,
                new SrsReviewMetadata(1450, (short) 1, (short) 0)
        );

        UserCardEntity updated = userCards.findById(card.getId()).orElseThrow();
        assertThat(updated.getState()).isEqualTo(CardState.valueOf(result.state().name()));
        assertThat(updated.getStability()).isEqualTo(result.stability());
        assertThat(updated.getDifficulty()).isEqualTo(result.difficulty());
        // PostgreSQL stores TIMESTAMPTZ at microsecond precision and rounds the
        // sub-microsecond tail, while Java Instant keeps nanoseconds — allow a
        // ±1µs window when comparing the round-tripped value.
        assertThat(updated.getDueDate()).isCloseTo(result.nextDue(), within(1, ChronoUnit.MICROS));
        assertThat(updated.getLastReview()).isNotNull();
        assertThat(updated.getReps()).isEqualTo(1);
        assertThat(updated.getLapses()).isEqualTo(0);
        assertThat(updated.getScheduledDays()).isEqualTo(result.scheduledDays());
        assertThat(updated.getElapsedDays()).isEqualTo(result.elapsedDays());

        assertThat(reviewLogs.findByUserCardIdOrderByReviewedAtAsc(card.getId()))
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.getUserCard().getId()).isEqualTo(card.getId());
                    assertThat(log.getRating()).isEqualTo((short) Rating.GOOD.value());
                    assertThat(log.getStateBefore()).isEqualTo(CardState.NEW);
                    assertThat(log.getStabilityBefore()).isEqualTo(0.5);
                    assertThat(log.getDifficultyBefore()).isEqualTo(5.0);
                    assertThat(log.getResponseTimeMs()).isEqualTo(1450);
                    assertThat(log.getHintCount()).isEqualTo((short) 1);
                    assertThat(log.getStrokeMistakes()).isEqualTo((short) 0);
                    assertThat(log.getReviewedAt()).isNotNull();
                });
    }

}
