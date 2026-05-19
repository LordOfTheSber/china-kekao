package dev.kekao.achievement;

import dev.kekao.achievement.AchievementDtos.AchievementListResponse;
import dev.kekao.achievement.AchievementDtos.AchievementView;
import dev.kekao.achievement.AchievementDtos.ClaimRequest;
import dev.kekao.study.CardState;
import dev.kekao.study.ReviewLogRepository;
import dev.kekao.study.UserCardRepository;
import dev.kekao.user.UserEntity;
import dev.kekao.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class AchievementService {

    private static final int STREAK_LOOKBACK_DAYS = 400;

    private final AchievementRepository achievements;
    private final UserAchievementRepository userAchievements;
    private final UserCardRepository userCards;
    private final ReviewLogRepository reviewLogs;
    private final UserRepository users;
    private final Clock clock;

    @Autowired
    public AchievementService(AchievementRepository achievements,
                              UserAchievementRepository userAchievements,
                              UserCardRepository userCards,
                              ReviewLogRepository reviewLogs,
                              UserRepository users) {
        this(achievements, userAchievements, userCards, reviewLogs, users, Clock.systemUTC());
    }

    AchievementService(AchievementRepository achievements,
                       UserAchievementRepository userAchievements,
                       UserCardRepository userCards,
                       ReviewLogRepository reviewLogs,
                       UserRepository users,
                       Clock clock) {
        this.achievements = achievements;
        this.userAchievements = userAchievements;
        this.userCards = userCards;
        this.reviewLogs = reviewLogs;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AchievementListResponse listForUser(Long userId) {
        List<AchievementEntity> all = achievements.findAllByOrderBySortOrderAsc();
        List<UserAchievementEntity> unlocked = userAchievements.findUnlockedForUser(userId);
        java.util.Map<Long, Instant> unlockTimes = new java.util.HashMap<>(unlocked.size());
        for (UserAchievementEntity ua : unlocked) {
            unlockTimes.put(ua.getAchievement().getId(), ua.getUnlockedAt());
        }
        List<AchievementView> views = new ArrayList<>(all.size());
        for (AchievementEntity a : all) {
            Instant unlockedAt = unlockTimes.get(a.getId());
            views.add(new AchievementView(
                    a.getCode(),
                    a.getName(),
                    a.getDescription(),
                    a.getGlyph(),
                    a.getCategory(),
                    a.getThreshold(),
                    a.getSortOrder(),
                    unlockedAt != null,
                    unlockedAt));
        }
        return new AchievementListResponse(views, unlockTimes.size(), all.size());
    }

    /**
     * Inspect numeric counters and unlock any threshold-based achievement the user newly satisfies.
     * Returns full views for the achievements unlocked during this call (possibly empty).
     */
    @Transactional
    public List<AchievementView> checkAfterReview(Long userId) {
        List<AchievementEntity> candidates = userAchievements.findLockedThresholdAchievements(userId);
        if (candidates.isEmpty()) return List.of();

        // Only run the counter queries actually needed for the locked candidates.
        boolean needLearned = false, needReviews = false, needStreak = false;
        for (AchievementEntity a : candidates) {
            switch (a.getCategory()) {
                case LEARNED -> needLearned = true;
                case STREAK -> needStreak = true;
                case REVIEWS -> needReviews = true;
                case SPECIAL -> { /* not eligible here */ }
            }
        }
        long learned = needLearned ? userCards.countByUserIdAndState(userId, CardState.REVIEW) : 0L;
        long reviewsTotal = needReviews ? reviewLogs.countByUserId(userId) : 0L;
        int streak = needStreak ? computeCurrentStreak(userId) : 0;

        List<AchievementView> unlocked = new ArrayList<>();
        for (AchievementEntity a : candidates) {
            boolean satisfied = switch (a.getCategory()) {
                case LEARNED -> learned >= a.getThreshold();
                case STREAK -> streak >= a.getThreshold();
                case REVIEWS -> reviewsTotal >= a.getThreshold();
                case SPECIAL -> false;
            };
            if (satisfied) {
                Instant unlockedAt = persistUnlockAt(userId, a);
                if (unlockedAt != null) {
                    unlocked.add(toView(a, true, unlockedAt));
                }
            }
        }
        return unlocked;
    }

    private AchievementView toView(AchievementEntity a, boolean unlocked, Instant unlockedAt) {
        return new AchievementView(
                a.getCode(),
                a.getName(),
                a.getDescription(),
                a.getGlyph(),
                a.getCategory(),
                a.getThreshold(),
                a.getSortOrder(),
                unlocked,
                unlockedAt);
    }

    /**
     * Claim a SPECIAL achievement from the client. Validates the claim against
     * the provided context. Returns the unlocked code if newly unlocked,
     * or empty otherwise.
     */
    @Transactional
    public Optional<AchievementView> claim(Long userId, String code, ClaimRequest req) {
        AchievementEntity a = achievements.findByCode(code).orElse(null);
        if (a == null) return Optional.empty();
        if (a.getCategory() != AchievementCategory.SPECIAL) return Optional.empty();
        if (userAchievements.existsByUserIdAndAchievementId(userId, a.getId())) {
            return Optional.empty();
        }
        if (!validateSpecial(a.getCode(), req)) {
            return Optional.empty();
        }
        Instant unlockedAt = persistUnlockAt(userId, a);
        if (unlockedAt != null) {
            return Optional.of(toView(a, true, unlockedAt));
        }
        return Optional.empty();
    }

    private boolean validateSpecial(String code, ClaimRequest req) {
        return switch (code) {
            case "PERFECT_DAY" -> req != null
                    && req.totalCount() != null && req.totalCount() >= 5
                    && req.againCount() != null && req.againCount() == 0
                    && req.hardCount() != null && req.hardCount() == 0;
            case "NIGHT_OWL" -> req != null
                    && req.localHour() != null
                    && req.localHour() >= 0 && req.localHour() < 5;
            case "EARLY_BIRD" -> req != null
                    && req.localHour() != null
                    && req.localHour() >= 5 && req.localHour() < 8;
            case "KONAMI", "PANDA" -> true; // pure discovery, no validation
            default -> false;
        };
    }

    private Instant persistUnlockAt(Long userId, AchievementEntity a) {
        UserEntity user = users.findById(userId).orElse(null);
        if (user == null) return null;
        if (userAchievements.existsByUserIdAndAchievementId(userId, a.getId())) {
            return null;
        }
        Instant when = Instant.now(clock);
        UserAchievementEntity ua = new UserAchievementEntity();
        ua.setUser(user);
        ua.setAchievement(a);
        ua.setUnlockedAt(when);
        try {
            userAchievements.save(ua);
            return when;
        } catch (org.springframework.dao.DataIntegrityViolationException ignored) {
            return null;
        }
    }

    private int computeCurrentStreak(Long userId) {
        Instant now = Instant.now(clock);
        Instant since = now.minus(STREAK_LOOKBACK_DAYS, ChronoUnit.DAYS);
        List<java.sql.Date> rows = reviewLogs.findDistinctReviewDays(userId, since);
        if (rows.isEmpty()) return 0;
        Set<java.time.LocalDate> days = new HashSet<>(rows.size());
        for (java.sql.Date d : rows) {
            days.add(d.toLocalDate());
        }
        java.time.LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
        java.time.LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        if (!days.contains(cursor)) return 0;
        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
