package dev.kekao.study;

/**
 * Optional telemetry attached to a review event.
 *
 * @param responseTimeMs latency between card display and submission
 * @param hintCount number of hints used by the learner
 * @param strokeMistakes number of stroke-order mistakes in production mode
 */
public record SrsReviewMetadata(
        Integer responseTimeMs,
        Short hintCount,
        Short strokeMistakes
) {

    public short safeHintCount() {
        return hintCount == null ? 0 : hintCount;
    }

    public short safeStrokeMistakes() {
        return strokeMistakes == null ? 0 : strokeMistakes;
    }

    public static SrsReviewMetadata empty() {
        return new SrsReviewMetadata(null, (short) 0, (short) 0);
    }
}
