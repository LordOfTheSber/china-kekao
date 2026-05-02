package dev.kekao.study.srs;

import java.time.Instant;

/**
 * Defines the contract for spaced-repetition scheduling implementations.
 *
 * <p>The algorithm receives the current card state, the user rating for the latest review,
 * and the current timestamp. It returns the next scheduling decision and updated card metrics.
 */
public interface SrsAlgorithm {

    /**
     * Calculates the next schedule for a card after a single review event.
     *
     * @param state current card state before applying the review result
     * @param rating user quality rating for the latest answer
     * @param now current point in time used as a baseline for due-date calculation
     * @return scheduling result with updated state and next due date
     */
    SrsScheduleResult schedule(SrsCardState state, Rating rating, Instant now);
}
