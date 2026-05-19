package dev.kekao.hanzi;

import dev.kekao.hanzi.HanziDtos.HanziDetailView;
import dev.kekao.hanzi.HanziDtos.HanziExampleView;
import dev.kekao.hanzi.HanziDtos.HanziSearchPage;
import dev.kekao.hanzi.HanziDtos.HanziSummary;
import dev.kekao.hanzi.HanziDtos.UserCardSummary;
import dev.kekao.study.UserCardEntity;
import dev.kekao.study.UserCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class HanziService {

    private static final String DEFAULT_LANGUAGE = "en";
    private static final int MAX_PAGE_SIZE = 100;

    private final HanziRepository hanzi;
    private final HanziTranslationRepository translations;
    private final HanziExampleRepository examples;
    private final UserCardRepository userCards;

    @Autowired
    public HanziService(HanziRepository hanzi,
                        HanziTranslationRepository translations,
                        HanziExampleRepository examples,
                        UserCardRepository userCards) {
        this.hanzi = hanzi;
        this.translations = translations;
        this.examples = examples;
        this.userCards = userCards;
    }

    @Transactional(readOnly = true)
    public HanziSearchPage search(String rawQuery, Short hskLevel, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = clamp(size, 1, MAX_PAGE_SIZE);
        String q = rawQuery == null ? "" : rawQuery.trim();
        String qLike = q.isEmpty() ? "%" : "%" + escape(q) + "%";
        String qPrefix = q.isEmpty() ? "%" : escape(q) + "%";

        long total = hanzi.countSearchPublished(q, qLike, hskLevel);
        List<HanziEntity> rows = hanzi.searchPublished(
                q, qPrefix, qLike, hskLevel, safeSize, safePage * safeSize);
        Map<Long, List<String>> meaningsByHanzi = loadMeanings(rows);
        List<HanziSummary> items = rows.stream()
                .map(h -> toSummary(h, meaningsByHanzi.getOrDefault(h.getId(), List.of())))
                .toList();
        return new HanziSearchPage(items, safePage, safeSize, total);
    }

    @Transactional(readOnly = true)
    public HanziDetailView detail(Long hanziId, Long userId) {
        HanziEntity entity = hanzi.findById(hanziId)
                .orElseThrow(() -> new NoSuchElementException("Hanzi not found: " + hanziId));
        List<String> meanings = findEnglishMeanings(entity.getId());
        List<HanziExampleView> exampleViews = examples.findByHanziIdAndLanguage(entity.getId(), DEFAULT_LANGUAGE).stream()
                .map(ex -> new HanziExampleView(ex.getSentence(), ex.getPinyin(), ex.getTranslation()))
                .toList();
        List<UserCardSummary> cards = userId == null ? List.of() :
                loadUserCards(userId, entity.getId());
        return new HanziDetailView(
                entity.getId(),
                entity.getCharacter(),
                entity.getPinyin(),
                entity.getStrokeCount(),
                entity.getHskLevel(),
                entity.getFrequencyRank(),
                meanings,
                entity.isHasStrokeData(),
                exampleViews,
                cards);
    }

    private List<UserCardSummary> loadUserCards(Long userId, Long hanziId) {
        UserCardEntity recognition = userCards
                .findByUserIdAndHanziIdAndMode(userId, hanziId, dev.kekao.study.StudyMode.RECOGNITION)
                .orElse(null);
        UserCardEntity production = userCards
                .findByUserIdAndHanziIdAndMode(userId, hanziId, dev.kekao.study.StudyMode.PRODUCTION)
                .orElse(null);
        java.util.List<UserCardSummary> out = new java.util.ArrayList<>();
        if (recognition != null) out.add(toSummary(recognition));
        if (production != null) out.add(toSummary(production));
        return out;
    }

    private static UserCardSummary toSummary(UserCardEntity card) {
        return new UserCardSummary(
                card.getId(),
                card.getMode().name(),
                card.getState().name(),
                card.getDueDate() == null ? null : card.getDueDate().toString());
    }

    private Map<Long, List<String>> loadMeanings(Collection<HanziEntity> rows) {
        if (rows.isEmpty()) return Map.of();
        List<Long> ids = rows.stream().map(HanziEntity::getId).distinct().toList();
        return translations.findMeaningsByHanziIdInAndLanguage(ids, DEFAULT_LANGUAGE).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> {
                            @SuppressWarnings("unchecked")
                            List<String> meanings = (List<String>) row[1];
                            return List.copyOf(meanings);
                        },
                        (a, b) -> a));
    }

    private List<String> findEnglishMeanings(Long hanziId) {
        return translations.findMeaningsByHanziIdAndLanguage(hanziId, DEFAULT_LANGUAGE).stream()
                .findFirst()
                .map(List::copyOf)
                .orElse(List.of());
    }

    private static HanziSummary toSummary(HanziEntity entity, List<String> meanings) {
        return new HanziSummary(
                entity.getId(),
                entity.getCharacter(),
                entity.getPinyin(),
                entity.getStrokeCount(),
                entity.getHskLevel(),
                entity.getFrequencyRank(),
                meanings,
                entity.isHasStrokeData());
    }

    private static String escape(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
