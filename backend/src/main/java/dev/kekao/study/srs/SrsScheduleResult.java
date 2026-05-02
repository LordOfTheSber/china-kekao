package dev.kekao.study.srs;

import java.time.Duration;
import java.time.Instant;

/**
 * Output returned by {@link SrsAlgorithm#schedule(SrsCardState, Rating, Instant)}.
 *
 * @param state resulting card state after processing the review
 * @param stability updated memory stability estimate
 * @param difficulty updated card difficulty estimate
 * @param nextDue timestamp when the card should be reviewed again
 * @param scheduledDays review interval expressed in whole days
 * @param elapsedDays elapsed interval since the previous review in whole days
 */
public record SrsScheduleResult(
        CardState state,
        double stability,
        double difficulty,
        Instant nextDue,
        int scheduledDays,
        int elapsedDays
) {

    /**
     * Returns the exact interval between the review time and the next due time.
     *
     * @param reviewedAt moment when the review was processed
     * @return duration until the next due timestamp
     */
    public Duration intervalFrom(Instant reviewedAt) {
        return Duration.between(reviewedAt, nextDue);
    }
}
