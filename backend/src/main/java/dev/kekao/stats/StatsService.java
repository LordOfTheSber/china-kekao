package dev.kekao.stats;

import dev.kekao.stats.StatsDtos.CardStateBreakdown;
import dev.kekao.stats.StatsDtos.DailyReview;
import dev.kekao.stats.StatsDtos.DashboardView;
import dev.kekao.stats.StatsDtos.OverviewView;
import dev.kekao.study.CardState;
import dev.kekao.study.ReviewLogRepository;
import dev.kekao.study.UserCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class StatsService {

    private static final int STREAK_LOOKBACK_DAYS = 365;

    private final UserCardRepository userCards;
    private final ReviewLogRepository reviewLogs;
    private final Clock clock;

    @Autowired
    public StatsService(UserCardRepository userCards, ReviewLogRepository reviewLogs) {
        this(userCards, reviewLogs, Clock.systemUTC());
    }

    StatsService(UserCardRepository userCards, ReviewLogRepository reviewLogs, Clock clock) {
        this.userCards = userCards;
        this.reviewLogs = reviewLogs;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public OverviewView overview(Long userId, int days) {
        int safeDays = Math.max(1, Math.min(days, 365));
        Instant now = Instant.now(clock);
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
        Instant since = today.minusDays(safeDays - 1L).atStartOfDay(ZoneOffset.UTC).toInstant();
        java.util.Map<LocalDate, long[]> byDay = new java.util.HashMap<>();
        for (Object[] row : reviewLogs.findDailyReviewCounts(userId, since)) {
            LocalDate day = ((java.sql.Date) row[0]).toLocalDate();
            long total = ((Number) row[1]).longValue();
            long good = ((Number) row[2]).longValue();
            byDay.put(day, new long[] {total, good});
        }
        java.util.List<DailyReview> daily = new java.util.ArrayList<>(safeDays);
        for (int i = 0; i < safeDays; i++) {
            LocalDate day = today.minusDays(safeDays - 1L - i);
            long[] vals = byDay.getOrDefault(day, new long[] {0, 0});
            double accuracy = vals[0] == 0 ? 0.0 : (double) vals[1] / vals[0];
            daily.add(new DailyReview(day.toString(), vals[0], vals[1], accuracy));
        }

        long news = 0, learning = 0, review = 0, relearning = 0;
        for (Object[] row : userCards.countCardsByState(userId)) {
            CardState state = (CardState) row[0];
            long count = ((Number) row[1]).longValue();
            switch (state) {
                case NEW -> news = count;
                case LEARNING -> learning = count;
                case REVIEW -> review = count;
                case RELEARNING -> relearning = count;
            }
        }
        return new OverviewView(safeDays, daily,
                new CardStateBreakdown(news, learning, review, relearning));
    }

    @Transactional(readOnly = true)
    public DashboardView dashboard(Long userId) {
        Instant now = Instant.now(clock);

        // One grouped query covers due-today (any state except NEW) and new-available counts
        // for cards belonging to the user's subscribed decks.
        long dueToday = 0;
        long newAvailable = 0;
        for (Object[] row : userCards.aggregateSubscribedCardCounts(userId, now)) {
            CardState state = (CardState) row[0];
            long dueNow = ((Number) row[1]).longValue();
            long total = ((Number) row[2]).longValue();
            if (state == CardState.NEW) {
                newAvailable = total;
            } else {
                dueToday += dueNow;
            }
        }
        long learnedTotal = userCards.countByUserIdAndState(userId, CardState.REVIEW);

        Instant streakWindow = now.minus(STREAK_LOOKBACK_DAYS, ChronoUnit.DAYS);
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);

        double accuracy = computeAccuracy(reviewLogs.findDailyReviewCounts(userId, sevenDaysAgo));
        int streak = computeCurrentStreak(reviewLogs.findDistinctReviewDays(userId, streakWindow), now);

        return new DashboardView(dueToday, newAvailable, learnedTotal, streak, accuracy);
    }

    private static double computeAccuracy(List<Object[]> dailyCounts) {
        if (dailyCounts.isEmpty()) return 0.0;
        long total = 0;
        long good = 0;
        for (Object[] row : dailyCounts) {
            total += ((Number) row[1]).longValue();
            good += ((Number) row[2]).longValue();
        }
        return total == 0 ? 0.0 : (double) good / total;
    }

    /**
     * Streak = number of consecutive UTC days, ending today or yesterday, on which the user
     * recorded at least one review. If the user has not reviewed today and not yesterday,
     * the streak is zero.
     */
    static int computeCurrentStreak(List<Date> reviewDayRows, Instant now) {
        if (reviewDayRows.isEmpty()) return 0;
        Set<LocalDate> reviewDays = new HashSet<>(reviewDayRows.size());
        for (Date d : reviewDayRows) {
            reviewDays.add(d.toLocalDate());
        }
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate cursor = reviewDays.contains(today) ? today : today.minusDays(1);
        if (!reviewDays.contains(cursor)) return 0;
        int streak = 0;
        while (reviewDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
