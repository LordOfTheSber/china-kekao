package dev.kekao.study.session;

import dev.kekao.hanzi.HanziEntity;
import dev.kekao.study.CardState;
import dev.kekao.study.StudyMode;
import dev.kekao.study.UserCardEntity;
import dev.kekao.study.UserCardRepository;
import dev.kekao.user.UserEntity;
import dev.kekao.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudySessionServiceTest {

    private static final long USER_ID = 42L;
    private static final Instant FIXED_NOW = Instant.parse("2026-05-03T12:00:00Z");

    @Mock private UserRepository users;
    @Mock private UserCardRepository userCards;

    private StudySessionService service;

    @BeforeEach
    void setUp() {
        service = new StudySessionService(
                users,
                userCards,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC),
                new Random(0)
        );
    }

    @Test
    void usesDefaultLimitsWhenSettingsMissing() {
        givenUser(new HashMap<>());
        when(userCards.findDueReviewCardsForUser(eq(USER_ID), eq(CardState.NEW), eq(FIXED_NOW), any(Pageable.class)))
                .thenReturn(List.of());
        when(userCards.findCardsInStateForUser(eq(USER_ID), eq(CardState.NEW), any(Pageable.class)))
                .thenReturn(List.of());

        service.getTodayQueue(USER_ID);

        ArgumentCaptor<Pageable> reviewPage = ArgumentCaptor.forClass(Pageable.class);
        verify(userCards).findDueReviewCardsForUser(eq(USER_ID), eq(CardState.NEW), eq(FIXED_NOW), reviewPage.capture());
        assertThat(reviewPage.getValue().getPageSize()).isEqualTo(StudySessionService.DEFAULT_MAX_REVIEWS_PER_DAY);

        ArgumentCaptor<Pageable> newPage = ArgumentCaptor.forClass(Pageable.class);
        verify(userCards).findCardsInStateForUser(eq(USER_ID), eq(CardState.NEW), newPage.capture());
        assertThat(newPage.getValue().getPageSize()).isEqualTo(StudySessionService.DEFAULT_NEW_PER_DAY);
    }

    @Test
    void appliesUserSettingsLimits() {
        givenUser(Map.of(
                StudySessionService.SETTING_NEW_PER_DAY, 5,
                StudySessionService.SETTING_MAX_REVIEWS_PER_DAY, 10
        ));
        when(userCards.findDueReviewCardsForUser(eq(USER_ID), eq(CardState.NEW), eq(FIXED_NOW), any(Pageable.class)))
                .thenReturn(List.of());
        when(userCards.findCardsInStateForUser(eq(USER_ID), eq(CardState.NEW), any(Pageable.class)))
                .thenReturn(List.of());

        service.getTodayQueue(USER_ID);

        ArgumentCaptor<Pageable> reviewPage = ArgumentCaptor.forClass(Pageable.class);
        verify(userCards).findDueReviewCardsForUser(eq(USER_ID), eq(CardState.NEW), eq(FIXED_NOW), reviewPage.capture());
        assertThat(reviewPage.getValue().getPageSize()).isEqualTo(10);

        ArgumentCaptor<Pageable> newPage = ArgumentCaptor.forClass(Pageable.class);
        verify(userCards).findCardsInStateForUser(eq(USER_ID), eq(CardState.NEW), newPage.capture());
        assertThat(newPage.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void zeroLimitsSkipQueriesEntirely() {
        givenUser(Map.of(
                StudySessionService.SETTING_NEW_PER_DAY, 0,
                StudySessionService.SETTING_MAX_REVIEWS_PER_DAY, 0
        ));

        List<UserCardEntity> queue = service.getTodayQueue(USER_ID);

        assertThat(queue).isEmpty();
        verify(userCards, never()).findDueReviewCardsForUser(any(), any(), any(), any());
        verify(userCards, never()).findCardsInStateForUser(any(), any(), any());
    }

    @Test
    void combinesReviewsAndNewCards() {
        givenUser(new HashMap<>());
        UserCardEntity review = card(1L, 100L, StudyMode.RECOGNITION, CardState.REVIEW);
        UserCardEntity newCard = card(2L, 200L, StudyMode.RECOGNITION, CardState.NEW);
        when(userCards.findDueReviewCardsForUser(eq(USER_ID), eq(CardState.NEW), eq(FIXED_NOW), any(Pageable.class)))
                .thenReturn(List.of(review));
        when(userCards.findCardsInStateForUser(eq(USER_ID), eq(CardState.NEW), any(Pageable.class)))
                .thenReturn(List.of(newCard));

        List<UserCardEntity> queue = service.getTodayQueue(USER_ID);

        assertThat(queue).extracting(UserCardEntity::getId).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void separatesSameHanziModesInQueue() {
        givenUser(new HashMap<>());
        // 3 hanzi, both modes for each — naive shuffle could pair them up.
        UserCardEntity a1 = card(1L, 100L, StudyMode.RECOGNITION, CardState.REVIEW);
        UserCardEntity a2 = card(2L, 100L, StudyMode.PRODUCTION, CardState.REVIEW);
        UserCardEntity b1 = card(3L, 200L, StudyMode.RECOGNITION, CardState.REVIEW);
        UserCardEntity b2 = card(4L, 200L, StudyMode.PRODUCTION, CardState.REVIEW);
        UserCardEntity c1 = card(5L, 300L, StudyMode.RECOGNITION, CardState.REVIEW);
        UserCardEntity c2 = card(6L, 300L, StudyMode.PRODUCTION, CardState.REVIEW);
        when(userCards.findDueReviewCardsForUser(eq(USER_ID), eq(CardState.NEW), eq(FIXED_NOW), any(Pageable.class)))
                .thenReturn(new ArrayList<>(List.of(a1, a2, b1, b2, c1, c2)));
        when(userCards.findCardsInStateForUser(eq(USER_ID), eq(CardState.NEW), any(Pageable.class)))
                .thenReturn(List.of());

        List<UserCardEntity> queue = service.getTodayQueue(USER_ID);

        assertThat(queue).hasSize(6);
        for (int i = 1; i < queue.size(); i++) {
            assertThat(queue.get(i).getHanzi().getId())
                    .as("cards at %d and %d must reference different hanzi", i - 1, i)
                    .isNotEqualTo(queue.get(i - 1).getHanzi().getId());
        }
    }

    @Test
    void separateSameHanziKeepsSinglePairOnEdges() {
        UserCardEntity a1 = card(1L, 100L, StudyMode.RECOGNITION, CardState.REVIEW);
        UserCardEntity a2 = card(2L, 100L, StudyMode.PRODUCTION, CardState.REVIEW);
        UserCardEntity b1 = card(3L, 200L, StudyMode.RECOGNITION, CardState.REVIEW);

        List<UserCardEntity> result = StudySessionService.separateSameHanzi(
                new ArrayList<>(List.of(a1, a2, b1))
        );

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getHanzi().getId()).isNotEqualTo(result.get(1).getHanzi().getId());
        assertThat(result.get(1).getHanzi().getId()).isNotEqualTo(result.get(2).getHanzi().getId());
    }

    @Test
    void unknownUserThrows() {
        when(users.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTodayQueue(USER_ID))
                .isInstanceOf(NoSuchElementException.class);
    }

    private void givenUser(Map<String, Object> settings) {
        UserEntity user = UserEntity.builder()
                .id(USER_ID)
                .email("u@example.com")
                .passwordHash("hash")
                .settings(settings)
                .build();
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
    }

    private UserCardEntity card(long id, long hanziId, StudyMode mode, CardState state) {
        HanziEntity hanzi = HanziEntity.builder().id(hanziId).character("x").build();
        return UserCardEntity.builder()
                .id(id)
                .hanzi(hanzi)
                .mode(mode)
                .state(state)
                .build();
    }
}
