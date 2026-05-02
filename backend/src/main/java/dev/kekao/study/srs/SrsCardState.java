package dev.kekao.study.srs;

import java.time.Instant;

/**
 * Snapshot of card scheduling data required by {@link SrsAlgorithm}.
 *
 * @param state current lifecycle state of the card
 * @param stability current memory stability estimate
 * @param difficulty current card difficulty estimate
 * @param dueDate when the card is currently due for review
 * @param reps number of successful repetitions
 * @param lapses number of failures after the card reached review state
 */
public record SrsCardState(
        CardState state,
        double stability,
        double difficulty,
        Instant dueDate,
        int reps,
        int lapses
) {
}
