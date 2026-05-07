package dev.kekao.study;

import dev.kekao.hanzi.HanziEntity;
import dev.kekao.study.srs.SrsAlgorithm;
import dev.kekao.study.srs.SrsCardState;
import dev.kekao.study.srs.SrsScheduleResult;
import dev.kekao.user.UserEntity;
import dev.kekao.user.UserSettings;
import dev.kekao.user.UserSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link SrsService} delegates to the injected default algorithm
 * for users on the default 0.9 retention, and switches to a per-user algorithm
 * when the value differs.
 */
@ExtendWith(MockitoExtension.class)
class SrsServicePerUserRetentionTest {

    @Mock private UserCardRepository userCards;
    @Mock private ReviewLogRepository reviewLogs;
    @Mock private SrsAlgorithm defaultAlgorithm;
    @Mock private UserSettingsService userSettings;

    @Test
    void usesDefaultAlgorithmWhenRetentionIsDefault() {
        SrsService service = new SrsService(userCards, reviewLogs, defaultAlgorithm, userSettings);
        when(userCards.findById(42L)).thenReturn(Optional.of(newCard(11L)));
        when(userSettings.get(11L)).thenReturn(new UserSettings(
                20, 200, 0.9, "DRAWING", "NORMAL", true));
        when(defaultAlgorithm.schedule(any(), any(), any())).thenReturn(stubResult());

        service.review(42L, Rating.GOOD, SrsReviewMetadata.empty());

        verify(defaultAlgorithm, times(1)).schedule(any(SrsCardState.class), any(), any(Instant.class));
    }

    @Test
    void usesPerUserAlgorithmWhenRetentionDiffers() {
        SrsService service = new SrsService(userCards, reviewLogs, defaultAlgorithm, userSettings);
        when(userCards.findById(7L)).thenReturn(Optional.of(newCard(99L)));
        when(userSettings.get(99L)).thenReturn(new UserSettings(
                20, 200, 0.95, "DRAWING", "NORMAL", true));

        service.review(7L, Rating.GOOD, SrsReviewMetadata.empty());

        // Default algorithm was bypassed in favour of an ad-hoc per-user one.
        verify(defaultAlgorithm, never()).schedule(any(), any(), any());
        verify(reviewLogs, times(1)).save(any());
    }

    @Test
    void fallsBackToDefaultWhenUserSettingsLookupFails() {
        SrsService service = new SrsService(userCards, reviewLogs, defaultAlgorithm, userSettings);
        when(userCards.findById(1L)).thenReturn(Optional.of(newCard(50L)));
        when(userSettings.get(50L)).thenThrow(new RuntimeException("boom"));
        when(defaultAlgorithm.schedule(any(), any(), any())).thenReturn(stubResult());

        service.review(1L, Rating.GOOD, SrsReviewMetadata.empty());

        verify(defaultAlgorithm).schedule(any(), any(), any());
    }

    @Test
    void worksWithoutUserSettingsService() {
        SrsService service = new SrsService(userCards, reviewLogs, defaultAlgorithm);
        when(userCards.findById(1L)).thenReturn(Optional.of(newCard(50L)));
        when(defaultAlgorithm.schedule(any(), any(), any())).thenReturn(stubResult());

        service.review(1L, Rating.GOOD, SrsReviewMetadata.empty());

        verify(defaultAlgorithm).schedule(any(), any(), any());
    }

    private UserCardEntity newCard(long ownerId) {
        UserEntity user = UserEntity.builder().id(ownerId).build();
        HanziEntity hanzi = HanziEntity.builder().id(1L).build();
        return UserCardEntity.builder()
                .id(1L)
                .user(user)
                .hanzi(hanzi)
                .mode(StudyMode.RECOGNITION)
                .state(CardState.REVIEW)
                .stability(1.0)
                .difficulty(5.0)
                .lastReview(Instant.now().minusSeconds(86400))
                .dueDate(Instant.now())
                .reps(1).lapses(0).elapsedDays(1).scheduledDays(1)
                .algorithmVersion("FSRS-5")
                .build();
    }

    private SrsScheduleResult stubResult() {
        Instant now = Instant.now();
        return new SrsScheduleResult(
                dev.kekao.study.srs.CardState.REVIEW,
                1.0, 5.0, now.plusSeconds(86400), 1, 1);
    }
}
