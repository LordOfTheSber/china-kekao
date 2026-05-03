package dev.kekao.study.grading;

import dev.kekao.hanzi.HanziEntity;
import dev.kekao.hanzi.HanziRepository;
import dev.kekao.hanzi.HanziTranslationEntity;
import dev.kekao.hanzi.HanziTranslationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Grades user answers in Recognition mode (TASK-014). Compares the learner's
 * pinyin and meaning against the canonical record, with tolerant normalization
 * and a near-match band (Levenshtein ≤ 1) for typos.
 */
@Service
public class GradingService {

    private static final String DEFAULT_LANGUAGE = "en";

    private final HanziRepository hanzi;
    private final HanziTranslationRepository translations;

    public GradingService(HanziRepository hanzi, HanziTranslationRepository translations) {
        this.hanzi = hanzi;
        this.translations = translations;
    }

    @Transactional(readOnly = true)
    public GradingResult gradeRecognition(Long hanziId,
                                          String userPinyin,
                                          String userMeaning,
                                          boolean withTones,
                                          boolean allowTypos) {
        HanziEntity entity = hanzi.findById(hanziId)
                .orElseThrow(() -> new NoSuchElementException("Hanzi not found: " + hanziId));
        List<String> meanings = translations.findByHanziId(hanziId).stream()
                .filter(t -> DEFAULT_LANGUAGE.equals(t.getLanguage()))
                .findFirst()
                .map(HanziTranslationEntity::getMeanings)
                .orElse(List.of());
        return grade(entity.getPinyin(), meanings, userPinyin, userMeaning, withTones, allowTypos);
    }

    /**
     * Pure version that operates on raw inputs — used by tests and reusable
     * outside the persistence path.
     */
    public static GradingResult grade(String expectedPinyin,
                                      List<String> acceptedMeanings,
                                      String userPinyin,
                                      String userMeaning,
                                      boolean withTones,
                                      boolean allowTypos) {
        boolean pinyinOk = pinyinMatches(expectedPinyin, userPinyin, withTones);
        if (!pinyinOk) return GradingResult.ofWrong();

        String normalizedAnswer = MeaningNormalizer.normalize(userMeaning);
        if (normalizedAnswer.isEmpty()) return GradingResult.ofWrong();

        int bestDistance = Integer.MAX_VALUE;
        for (String accepted : acceptedMeanings) {
            String normalizedAccepted = MeaningNormalizer.normalize(accepted);
            if (normalizedAccepted.isEmpty()) continue;
            if (normalizedAccepted.equals(normalizedAnswer)) return GradingResult.ofCorrect();
            int d = levenshtein(normalizedAccepted, normalizedAnswer);
            if (d < bestDistance) bestDistance = d;
        }
        if (allowTypos && bestDistance <= 1) return GradingResult.ofNear();
        return GradingResult.ofWrong();
    }

    private static boolean pinyinMatches(String expected, String actual, boolean withTones) {
        String e = PinyinNormalizer.toNumeric(expected, withTones);
        String a = PinyinNormalizer.toNumeric(actual, withTones);
        return !e.isEmpty() && e.equals(a);
    }

    static int levenshtein(String a, String b) {
        int n = a.length();
        int m = b.length();
        if (n == 0) return m;
        if (m == 0) return n;
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) prev[j] = j;
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[m];
    }
}
