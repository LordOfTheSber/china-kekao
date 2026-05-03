package dev.kekao.study;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class StudyDtos {

    private StudyDtos() {}

    /**
     * Card payload returned to the client for a study session.
     * <ul>
     *   <li>RECOGNITION: {@code character} and {@code pinyin} are populated; {@code meanings} stay {@code null}.</li>
     *   <li>PRODUCTION: {@code meanings} are populated; {@code character} is hidden so the learner can draw it.
     *       {@code strokeData} is reserved for the stroke-order payload (added in a later task).</li>
     * </ul>
     */
    public record StudyCardView(
            Long userCardId,
            Long hanziId,
            String character,
            String pinyin,
            StudyMode mode,
            List<String> meanings,
            String strokeData
    ) {}

    public record ReviewRequest(
            @NotNull Long userCardId,
            @NotNull StudyMode mode,
            @NotNull Rating rating,
            @Min(0) Integer responseTimeMs,
            @Min(0) Short hintCount,
            @Min(0) Short strokeMistakes
    ) {}

    public record ReviewResponse(
            Long userCardId,
            CardState state,
            double stability,
            double difficulty,
            Instant nextDue,
            int scheduledDays,
            int elapsedDays
    ) {}
}
