package dev.kekao.study;

import dev.kekao.hanzi.HanziEntity;
import dev.kekao.hanzi.HanziTranslationEntity;
import dev.kekao.hanzi.HanziTranslationRepository;
import dev.kekao.study.StudyDtos.ReviewRequest;
import dev.kekao.study.StudyDtos.ReviewResponse;
import dev.kekao.study.StudyDtos.StudyCardView;
import dev.kekao.study.session.StudySessionService;
import dev.kekao.study.srs.SrsScheduleResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/study")
public class StudyController {

    private static final String DEFAULT_LANGUAGE = "en";

    private final StudySessionService sessionService;
    private final SrsService srsService;
    private final UserCardRepository userCards;
    private final HanziTranslationRepository translations;

    public StudyController(StudySessionService sessionService,
                           SrsService srsService,
                           UserCardRepository userCards,
                           HanziTranslationRepository translations) {
        this.sessionService = sessionService;
        this.srsService = srsService;
        this.userCards = userCards;
        this.translations = translations;
    }

    @GetMapping("/session")
    public List<StudyCardView> session(@AuthenticationPrincipal Jwt jwt) {
        long userId = userId(jwt);
        List<UserCardEntity> queue = sessionService.getTodayQueue(userId);
        Map<Long, List<String>> meaningsByHanzi = loadEnglishMeanings(queue);
        return queue.stream().map(card -> toView(card, meaningsByHanzi)).toList();
    }

    @PostMapping("/review")
    public ResponseEntity<ReviewResponse> review(@AuthenticationPrincipal Jwt jwt,
                                                 @Valid @RequestBody ReviewRequest req) {
        long userId = userId(jwt);
        UserCardEntity card = userCards.findById(req.userCardId())
                .orElseThrow(() -> new UserCardNotFoundException(req.userCardId()));
        if (!card.getUser().getId().equals(userId)) {
            throw new ReviewForbiddenException();
        }
        if (card.getMode() != req.mode()) {
            throw new ReviewModeMismatchException();
        }

        SrsReviewMetadata metadata = new SrsReviewMetadata(
                req.responseTimeMs(),
                req.hintCount(),
                req.strokeMistakes());
        SrsScheduleResult result = srsService.review(req.userCardId(), req.rating(), metadata);

        ReviewResponse body = new ReviewResponse(
                req.userCardId(),
                CardState.valueOf(result.state().name()),
                result.stability(),
                result.difficulty(),
                result.nextDue(),
                result.scheduledDays(),
                result.elapsedDays());
        return ResponseEntity.ok(body);
    }

    private Map<Long, List<String>> loadEnglishMeanings(List<UserCardEntity> cards) {
        List<Long> hanziIds = cards.stream()
                .filter(c -> c.getMode() == StudyMode.PRODUCTION)
                .map(c -> c.getHanzi().getId())
                .distinct()
                .toList();
        if (hanziIds.isEmpty()) return Map.of();
        return translations.findByHanziIdInAndLanguage(hanziIds, DEFAULT_LANGUAGE).stream()
                .collect(Collectors.toMap(
                        t -> t.getHanzi().getId(),
                        t -> List.copyOf(t.getMeanings()),
                        (a, b) -> a));
    }

    private StudyCardView toView(UserCardEntity card, Map<Long, List<String>> meaningsByHanzi) {
        HanziEntity hanzi = card.getHanzi();
        if (card.getMode() == StudyMode.RECOGNITION) {
            return new StudyCardView(
                    card.getId(),
                    hanzi.getId(),
                    hanzi.getCharacter(),
                    hanzi.getPinyin(),
                    StudyMode.RECOGNITION,
                    null,
                    null);
        }
        return new StudyCardView(
                card.getId(),
                hanzi.getId(),
                null,
                hanzi.getPinyin(),
                StudyMode.PRODUCTION,
                meaningsByHanzi.getOrDefault(hanzi.getId(), List.of()),
                null);
    }

    private static long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    public static class ReviewForbiddenException extends RuntimeException {}

    public static class ReviewModeMismatchException extends RuntimeException {}
}
