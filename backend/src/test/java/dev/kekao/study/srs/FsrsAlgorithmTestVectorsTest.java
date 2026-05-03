package dev.kekao.study.srs;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FsrsAlgorithmTestVectorsTest {

    private static final double EPSILON = 0.0001;
    private static final Instant NOW = Instant.parse("2026-05-03T00:00:00Z");

    private final FsrsAlgorithm algorithm = new FsrsAlgorithm(0.9);

    @ParameterizedTest(name = "vector #{index}: {0}")
    @MethodSource("vectors")
    void shouldMatchReferenceVectors(TestVector vector) {
        SrsScheduleResult actual = algorithm.schedule(vector.input(), vector.rating(), NOW);

        assertThat(actual.state()).isEqualTo(vector.expectedState());
        assertThat(actual.stability()).isCloseTo(vector.expectedStability(), withinTolerance());
        assertThat(actual.difficulty()).isCloseTo(vector.expectedDifficulty(), withinTolerance());
        assertThat(actual.elapsedDays()).isEqualTo(vector.expectedElapsedDays());
        assertThat(actual.scheduledDays()).isEqualTo(vector.expectedScheduledDays());
    }

    private org.assertj.core.data.Offset<Double> withinTolerance() {
        return org.assertj.core.data.Offset.offset(EPSILON);
    }

    private static Stream<TestVector> vectors() {
        return Stream.of(
                vector("new-again", newState(CardState.NEW, 0, 0, null, 0, 0), Rating.AGAIN,
                        CardState.LEARNING, 0.4872, 5.1618, 0, 1),
                vector("new-hard", newState(CardState.NEW, 0, 0, null, 0, 0), Rating.HARD,
                        CardState.LEARNING, 1.4003, 2.7413, 0, 1),
                vector("new-good", newState(CardState.NEW, 0, 0, null, 0, 0), Rating.GOOD,
                        CardState.LEARNING, 3.7145, 1.0, 0, 4),
                vector("new-easy", newState(CardState.NEW, 0, 0, null, 0, 0), Rating.EASY,
                        CardState.REVIEW, 13.8206, 1.0, 0, 14),
                vector("review-a-again", state(CardState.REVIEW, 2.5, 6.2, "2026-04-30T00:00:00Z", 5, 1), Rating.AGAIN,
                        CardState.RELEARNING, 1.0813, 7.7782, 3, 1),
                vector("review-a-hard", state(CardState.REVIEW, 2.5, 6.2, "2026-04-30T00:00:00Z", 5, 1), Rating.HARD,
                        CardState.REVIEW, 3.8817, 6.9085, 3, 4),
                vector("review-a-good", state(CardState.REVIEW, 2.5, 6.2, "2026-04-30T00:00:00Z", 5, 1), Rating.GOOD,
                        CardState.REVIEW, 9.8743, 6.0388, 3, 10),
                vector("review-a-easy", state(CardState.REVIEW, 2.5, 6.2, "2026-04-30T00:00:00Z", 5, 1), Rating.EASY,
                        CardState.REVIEW, 27.4220, 5.1691, 3, 27),
                vector("review-b-again", state(CardState.REVIEW, 8.1, 4.4, "2026-04-20T00:00:00Z", 20, 2), Rating.AGAIN,
                        CardState.RELEARNING, 2.4204, 6.0340, 13, 1),
                vector("review-b-hard", state(CardState.REVIEW, 8.1, 4.4, "2026-04-20T00:00:00Z", 20, 2), Rating.HARD,
                        CardState.REVIEW, 15.0990, 5.1643, 13, 15),
                vector("review-b-good", state(CardState.REVIEW, 8.1, 4.4, "2026-04-20T00:00:00Z", 20, 2), Rating.GOOD,
                        CardState.REVIEW, 43.4965, 4.2946, 13, 43),
                vector("review-b-easy", state(CardState.REVIEW, 8.1, 4.4, "2026-04-20T00:00:00Z", 20, 2), Rating.EASY,
                        CardState.REVIEW, 123.0836, 3.4249, 13, 123),
                vector("learning-again", state(CardState.LEARNING, 1.8, 7.3, "2026-05-01T00:00:00Z", 3, 0), Rating.AGAIN,
                        CardState.RELEARNING, 0.8368, 8.8441, 2, 1),
                vector("learning-hard", state(CardState.LEARNING, 1.8, 7.3, "2026-05-01T00:00:00Z", 3, 0), Rating.HARD,
                        CardState.REVIEW, 2.5187, 7.9744, 2, 3),
                vector("learning-good", state(CardState.LEARNING, 1.8, 7.3, "2026-05-01T00:00:00Z", 3, 0), Rating.GOOD,
                        CardState.REVIEW, 5.8724, 7.1047, 2, 6),
                vector("learning-easy", state(CardState.LEARNING, 1.8, 7.3, "2026-05-01T00:00:00Z", 3, 0), Rating.EASY,
                        CardState.REVIEW, 16.1247, 6.2350, 2, 16),
                vector("relearning-again", state(CardState.RELEARNING, 1.2, 8.0, "2026-04-28T00:00:00Z", 7, 4), Rating.AGAIN,
                        CardState.RELEARNING, 0.8130, 9.5224, 5, 1),
                vector("relearning-hard", state(CardState.RELEARNING, 1.2, 8.0, "2026-04-28T00:00:00Z", 7, 4), Rating.HARD,
                        CardState.REVIEW, 2.3437, 8.6527, 5, 2),
                vector("relearning-good", state(CardState.RELEARNING, 1.2, 8.0, "2026-04-28T00:00:00Z", 7, 4), Rating.GOOD,
                        CardState.REVIEW, 8.0991, 7.7830, 5, 8),
                vector("relearning-easy", state(CardState.RELEARNING, 1.2, 8.0, "2026-04-28T00:00:00Z", 7, 4), Rating.EASY,
                        CardState.REVIEW, 26.4013, 6.9133, 5, 26)
        );
    }

    private static TestVector vector(
            String name,
            SrsCardState input,
            Rating rating,
            CardState expectedState,
            double expectedStability,
            double expectedDifficulty,
            int expectedElapsedDays,
            int expectedScheduledDays
    ) {
        return new TestVector(name, input, rating, expectedState, expectedStability, expectedDifficulty,
                expectedElapsedDays, expectedScheduledDays);
    }

    private static SrsCardState state(CardState state, double stability, double difficulty, String dueDate, int reps, int lapses) {
        return new SrsCardState(state, stability, difficulty, Instant.parse(dueDate), reps, lapses);
    }

    private static SrsCardState newState(CardState state, double stability, double difficulty, Instant dueDate, int reps, int lapses) {
        return new SrsCardState(state, stability, difficulty, dueDate, reps, lapses);
    }

    private record TestVector(
            String name,
            SrsCardState input,
            Rating rating,
            CardState expectedState,
            double expectedStability,
            double expectedDifficulty,
            int expectedElapsedDays,
            int expectedScheduledDays
    ) {
        @Override
        public String toString() {
            return name;
        }
    }
}
