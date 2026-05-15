package dev.kekao.achievement;

import java.time.Instant;
import java.util.List;

public final class AchievementDtos {

    private AchievementDtos() {}

    public record AchievementView(
            String code,
            String name,
            String description,
            String glyph,
            AchievementCategory category,
            Long threshold,
            int sortOrder,
            boolean unlocked,
            Instant unlockedAt
    ) {}

    public record AchievementListResponse(
            List<AchievementView> items,
            int unlocked,
            int total
    ) {}

    public record ClaimRequest(
            // Optional context for SPECIAL achievements claimed from the client.
            // For PERFECT_DAY the client passes the rating breakdown so the server
            // can validate. For NIGHT_OWL/EARLY_BIRD it passes the local hour.
            Integer localHour,
            Integer againCount,
            Integer hardCount,
            Integer totalCount
    ) {}
}
