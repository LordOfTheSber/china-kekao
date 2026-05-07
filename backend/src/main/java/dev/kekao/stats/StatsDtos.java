package dev.kekao.stats;

import java.util.List;

public final class StatsDtos {

    private StatsDtos() {}

    public record DashboardView(
            long dueTodayCount,
            long newAvailableCount,
            long learnedTotal,
            int currentStreak,
            double accuracy7d
    ) {}

    public record DailyReview(
            String date,
            long total,
            long good,
            double accuracy
    ) {}

    public record OverviewView(
            int days,
            List<DailyReview> daily,
            CardStateBreakdown states
    ) {}

    public record CardStateBreakdown(
            long newCount,
            long learning,
            long review,
            long relearning
    ) {}
}
