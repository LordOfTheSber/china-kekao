package dev.kekao.study;

import dev.kekao.study.srs.SrsAlgorithm;
import dev.kekao.study.srs.SrsCardState;
import dev.kekao.study.srs.SrsScheduleResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class SrsService {

    private static final String ALGORITHM_VERSION = "FSRS-5";

    private final UserCardRepository userCards;
    private final ReviewLogRepository reviewLogs;
    private final SrsAlgorithm algorithm;

    @Autowired
    public SrsService(UserCardRepository userCards, ReviewLogRepository reviewLogs, SrsAlgorithm algorithm) {
        this.userCards = userCards;
        this.reviewLogs = reviewLogs;
        this.algorithm = algorithm;
    }

    @Transactional
    public SrsScheduleResult review(Long userCardId, Rating rating, SrsReviewMetadata metadata) {
        Instant now = Instant.now();
        UserCardEntity card = loadCard(userCardId);
        SrsCardState state = toAlgorithmState(card);
        SrsScheduleResult result = algorithm.schedule(state, toAlgorithmRating(rating), now);
        persistCardState(card, result, now, state, rating);
        persistReviewLog(card, state, result, rating, metadata, now);
        return result;
    }

    private UserCardEntity loadCard(Long userCardId) {
        return userCards.findById(userCardId)
                .orElseThrow(() -> new UserCardNotFoundException(userCardId));
    }

    private SrsCardState toAlgorithmState(UserCardEntity card) {
        return new SrsCardState(
                dev.kekao.study.srs.CardState.valueOf(card.getState().name()),
                card.getStability(),
                card.getDifficulty(),
                card.getDueDate(),
                card.getReps(),
                card.getLapses()
        );
    }

    private dev.kekao.study.srs.Rating toAlgorithmRating(Rating rating) {
        return dev.kekao.study.srs.Rating.valueOf(rating.name());
    }

    private void persistCardState(
            UserCardEntity card,
            SrsScheduleResult result,
            Instant now,
            SrsCardState state,
            Rating rating
    ) {
        card.setState(CardState.valueOf(result.state().name()));
        card.setStability(result.stability());
        card.setDifficulty(result.difficulty());
        card.setLastReview(now);
        card.setDueDate(result.nextDue());
        card.setElapsedDays(result.elapsedDays());
        card.setScheduledDays(result.scheduledDays());
        card.setAlgorithmVersion(ALGORITHM_VERSION);
        card.setReps(state.reps() + 1);
        card.setLapses(nextLapses(card.getLapses(), state.state(), rating));
    }

    private int nextLapses(int currentLapses, dev.kekao.study.srs.CardState previousState, Rating rating) {
        boolean failedReviewCard = rating == Rating.AGAIN && previousState != dev.kekao.study.srs.CardState.NEW;
        return failedReviewCard ? currentLapses + 1 : currentLapses;
    }

    private void persistReviewLog(
            UserCardEntity card,
            SrsCardState state,
            SrsScheduleResult result,
            Rating rating,
            SrsReviewMetadata metadata,
            Instant now
    ) {
        SrsReviewMetadata safeMetadata = metadata == null ? SrsReviewMetadata.empty() : metadata;
        ReviewLogEntity log = ReviewLogEntity.builder()
                .userCard(card)
                .rating((short) rating.value())
                .stateBefore(CardState.valueOf(state.state().name()))
                .elapsedDays(result.elapsedDays())
                .scheduledDays(result.scheduledDays())
                .stabilityBefore(state.stability())
                .difficultyBefore(state.difficulty())
                .reviewedAt(now)
                .responseTimeMs(safeMetadata.responseTimeMs())
                .hintCount(safeMetadata.safeHintCount())
                .strokeMistakes(safeMetadata.safeStrokeMistakes())
                .build();
        reviewLogs.save(log);
    }
}
