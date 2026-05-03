package dev.kekao.study.grading;

import dev.kekao.study.Rating;

/**
 * Outcome of grading a Recognition answer.
 *
 * @param correct true when both pinyin and meaning fully match
 * @param nearMatch true when pinyin matches but the meaning is within Levenshtein 1
 *                  of one of the accepted meanings — UI should ask the user to confirm
 * @param suggestedRating recommended FSRS rating to feed into the SRS service when
 *                        the user accepts the grading verdict (defaults: GOOD when
 *                        correct, HARD on near-match, AGAIN otherwise)
 */
public record GradingResult(boolean correct, boolean nearMatch, Rating suggestedRating) {

    public static GradingResult ofCorrect() {
        return new GradingResult(true, false, Rating.GOOD);
    }

    public static GradingResult ofNear() {
        return new GradingResult(false, true, Rating.HARD);
    }

    public static GradingResult ofWrong() {
        return new GradingResult(false, false, Rating.AGAIN);
    }
}
