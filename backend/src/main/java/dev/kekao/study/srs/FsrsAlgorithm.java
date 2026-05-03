package dev.kekao.study.srs;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.Map;

/**
 * FSRS-5 implementation with default parameters from the official specification.
 */
public final class FsrsAlgorithm implements SrsAlgorithm {

    private static final double[] DEFAULT_WEIGHTS = {
            0.4872, 1.4003, 3.7145, 13.8206,
            5.1618, 1.2298, 0.8975, 0.031,
            1.6474, 0.1367, 1.0461, 2.1072,
            0.0793, 0.3246, 1.587, 0.2272,
            2.8755, 0.1624, 0.9704
    };

    private static final double DECAY = -0.5;
    private static final double FACTOR = 19.0 / 81.0;
    private static final int MIN_INTERVAL_DAYS = 1;
    private static final int MAX_INTERVAL_DAYS = 36500;

    private final double requestRetention;
    private final double[] w;

    public FsrsAlgorithm() {
        this(0.9);
    }

    public FsrsAlgorithm(double requestRetention) {
        this(requestRetention, DEFAULT_WEIGHTS);
    }

    FsrsAlgorithm(double requestRetention, double[] weights) {
        validateRetention(requestRetention);
        this.requestRetention = requestRetention;
        this.w = weights.clone();
    }

    @Override
    public SrsScheduleResult schedule(SrsCardState state, Rating rating, Instant now) {
        int elapsedDays = elapsedDays(state.dueDate(), now);
        if (state.state() == CardState.NEW) {
            return scheduleNewCard(rating, now, elapsedDays);
        }
        return scheduleReviewedCard(state, rating, now, elapsedDays);
    }

    private SrsScheduleResult scheduleNewCard(Rating rating, Instant now, int elapsedDays) {
        double stability = initialStability(rating);
        double difficulty = initialDifficulty(rating);
        if (rating == Rating.AGAIN || rating == Rating.HARD) {
            return buildResult(CardState.LEARNING, stability, difficulty, now, MIN_INTERVAL_DAYS, elapsedDays);
        }
        int interval = nextIntervalDays(stability);
        CardState nextState = rating == Rating.EASY ? CardState.REVIEW : CardState.LEARNING;
        return buildResult(nextState, stability, difficulty, now, interval, elapsedDays);
    }

    private SrsScheduleResult scheduleReviewedCard(
            SrsCardState state,
            Rating rating,
            Instant now,
            int elapsedDays
    ) {
        double retrievability = retrievability(state.stability(), elapsedDays);
        double difficulty = nextDifficulty(state.difficulty(), rating);
        if (rating == Rating.AGAIN) {
            double stability = nextForgetStability(state.stability(), difficulty, retrievability);
            return buildResult(CardState.RELEARNING, stability, difficulty, now, MIN_INTERVAL_DAYS, elapsedDays);
        }
        double stability = nextRecallStability(state.stability(), difficulty, retrievability, rating);
        int interval = nextIntervalDays(stability);
        CardState nextState = interval > MIN_INTERVAL_DAYS ? CardState.REVIEW : CardState.LEARNING;
        return buildResult(nextState, stability, difficulty, now, interval, elapsedDays);
    }

    private SrsScheduleResult buildResult(
            CardState state,
            double stability,
            double difficulty,
            Instant now,
            int intervalDays,
            int elapsedDays
    ) {
        int boundedInterval = clampInterval(intervalDays);
        return new SrsScheduleResult(
                state,
                stability,
                difficulty,
                now.plus(boundedInterval, ChronoUnit.DAYS),
                boundedInterval,
                elapsedDays
        );
    }

    private double initialStability(Rating rating) {
        Map<Rating, Double> ratings = new EnumMap<>(Rating.class);
        ratings.put(Rating.AGAIN, w[0]);
        ratings.put(Rating.HARD, w[1]);
        ratings.put(Rating.GOOD, w[2]);
        ratings.put(Rating.EASY, w[3]);
        return ratings.get(rating);
    }

    private double initialDifficulty(Rating rating) {
        double base = w[4] - Math.exp((rating.value() - 1) * w[5]) + 1;
        return clampDifficulty(base);
    }

    private double nextDifficulty(double currentDifficulty, Rating rating) {
        double raw = currentDifficulty - w[6] * (rating.value() - Rating.GOOD.value());
        double reverted = w[7] * initialDifficulty(Rating.EASY) + (1 - w[7]) * raw;
        return clampDifficulty(reverted);
    }

    private double nextRecallStability(double stability, double difficulty, double retrievability, Rating rating) {
        double hardPenalty = rating == Rating.HARD ? w[15] : 1;
        double easyBonus = rating == Rating.EASY ? w[16] : 1;
        double gain = Math.exp(w[8])
                * (11 - difficulty)
                * Math.pow(stability, -w[9])
                * (Math.exp((1 - retrievability) * w[10]) - 1)
                * hardPenalty
                * easyBonus;
        return stability * (1 + gain);
    }

    private double nextForgetStability(double stability, double difficulty, double retrievability) {
        return w[11]
                * Math.pow(difficulty, -w[12])
                * (Math.pow(stability + 1, w[13]) - 1)
                * Math.exp((1 - retrievability) * w[14]);
    }

    private double retrievability(double stability, int elapsedDays) {
        return Math.pow(1 + FACTOR * elapsedDays / stability, DECAY);
    }

    private int nextIntervalDays(double stability) {
        double raw = (stability / FACTOR) * (Math.pow(requestRetention, 1 / DECAY) - 1);
        return clampInterval((int) Math.round(raw));
    }

    private int elapsedDays(Instant dueDate, Instant now) {
        if (dueDate == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(dueDate, now);
        return (int) Math.max(days, 0);
    }

    private int clampInterval(int days) {
        return Math.max(MIN_INTERVAL_DAYS, Math.min(days, MAX_INTERVAL_DAYS));
    }

    private double clampDifficulty(double value) {
        return Math.max(1.0, Math.min(value, 10.0));
    }

    private void validateRetention(double retention) {
        if (retention <= 0 || retention >= 1) {
            throw new IllegalArgumentException("requestRetention must be in range (0, 1)");
        }
    }
}
