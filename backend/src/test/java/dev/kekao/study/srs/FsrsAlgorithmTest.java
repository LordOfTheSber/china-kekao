package dev.kekao.study.srs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class FsrsAlgorithmTest {

    private final FsrsAlgorithm algorithm = new FsrsAlgorithm(0.9);

    @Test
    void shouldScheduleNewGoodCardToLearning() {
        Instant now = Instant.parse("2026-05-02T10:00:00Z");
        SrsCardState state = new SrsCardState(CardState.NEW, 0, 0, null, 0, 0);

        SrsScheduleResult result = algorithm.schedule(state, Rating.GOOD, now);

        assertThat(result.state()).isEqualTo(CardState.LEARNING);
        assertThat(result.stability()).isGreaterThan(0);
        assertThat(result.difficulty()).isBetween(1.0, 10.0);
        assertThat(result.scheduledDays()).isGreaterThanOrEqualTo(1);
        assertThat(result.nextDue()).isAfter(now);
    }

    @Test
    void shouldMoveReviewCardToRelearningAfterAgain() {
        Instant due = Instant.parse("2026-04-25T10:00:00Z");
        Instant now = Instant.parse("2026-05-02T10:00:00Z");
        SrsCardState state = new SrsCardState(CardState.REVIEW, 12.0, 5.0, due, 10, 0);

        SrsScheduleResult result = algorithm.schedule(state, Rating.AGAIN, now);

        assertThat(result.state()).isEqualTo(CardState.RELEARNING);
        assertThat(result.scheduledDays()).isEqualTo(1);
        assertThat(result.elapsedDays()).isEqualTo(7);
    }

    @Test
    void shouldRejectInvalidRequestRetention() {
        assertThatThrownBy(() -> new FsrsAlgorithm(1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requestRetention");
    }
}
