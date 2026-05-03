package dev.kekao.stats;

public final class StatsDtos {

    private StatsDtos() {}

    public record DashboardView(
            long dueTodayCount,
            long newAvailableCount,
            long learnedTotal,
            int currentStreak,
            double accuracy7d
    ) {}
}
