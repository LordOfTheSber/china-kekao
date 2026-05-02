package dev.kekao.study.srs;

/**
 * Lifecycle states for cards managed by spaced-repetition algorithms.
 */
public enum CardState {
    NEW,
    LEARNING,
    REVIEW,
    RELEARNING
}
