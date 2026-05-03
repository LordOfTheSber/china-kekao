package dev.kekao.study.grading;

import dev.kekao.study.Rating;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GradingServiceTest {

    private static final List<String> NI_HAO_MEANINGS = List.of("you", "thou");
    private static final List<String> SHAN_MEANINGS = List.of("mountain", "hill");
    private static final List<String> ARTICLE_MEANINGS = List.of("the apple", "fruit");

    @Nested
    class PinyinFormats {

        @ParameterizedTest
        @CsvSource({
                "nǐ, nǐ",
                "nǐ, ni3",
                "ni3, ni3",
                "ni3, nǐ",
                " Ni3 , nǐ",
                "NǏ, ni3"
        })
        void acceptsBothToneFormats(String expected, String userInput) {
            GradingResult r = GradingService.grade(expected, NI_HAO_MEANINGS,
                    userInput, "you", true, true);
            assertThat(r.correct()).isTrue();
        }

        @Test
        void rejectsWrongToneWhenWithTonesEnabled() {
            GradingResult r = GradingService.grade("nǐ", NI_HAO_MEANINGS, "ni2", "you", true, true);
            assertThat(r.correct()).isFalse();
            assertThat(r.suggestedRating()).isEqualTo(Rating.AGAIN);
        }

        @Test
        void ignoresTonesWhenWithTonesDisabled() {
            GradingResult r = GradingService.grade("nǐ", NI_HAO_MEANINGS, "ni2", "you", false, true);
            assertThat(r.correct()).isTrue();
        }

        @Test
        void ignoresTonesAcceptsBareSyllable() {
            GradingResult r = GradingService.grade("nǐ", NI_HAO_MEANINGS, "ni", "you", false, true);
            assertThat(r.correct()).isTrue();
        }

        @Test
        void wrongSyllableNeverMatches() {
            GradingResult r = GradingService.grade("nǐ", NI_HAO_MEANINGS, "ma3", "you", true, true);
            assertThat(r.correct()).isFalse();
            assertThat(r.nearMatch()).isFalse();
        }

        @Test
        void emptyPinyinFails() {
            GradingResult r = GradingService.grade("nǐ", NI_HAO_MEANINGS, "", "you", true, true);
            assertThat(r.correct()).isFalse();
        }

        @Test
        void nullPinyinFails() {
            GradingResult r = GradingService.grade("nǐ", NI_HAO_MEANINGS, null, "you", true, true);
            assertThat(r.correct()).isFalse();
        }

        @Test
        void multiSyllablePinyinMatches() {
            GradingResult r = GradingService.grade("nǐ hǎo", List.of("hello"),
                    "ni3 hao3", "hello", true, true);
            assertThat(r.correct()).isTrue();
        }

        @Test
        void uTreatedAsVForUmlaut() {
            // 女 — nǚ; numeric form uses 'v' for ü
            GradingResult r = GradingService.grade("nǚ", List.of("woman"),
                    "nv3", "woman", true, true);
            assertThat(r.correct()).isTrue();
        }

        @Test
        void uppercaseUmlautNormalizes() {
            GradingResult r = GradingService.grade("Nǚ", List.of("woman"),
                    "Nv3", "woman", true, true);
            assertThat(r.correct()).isTrue();
        }
    }

    @Nested
    class MeaningExactMatch {

        @Test
        void exactSingleMeaningMatches() {
            GradingResult r = GradingService.grade("nǐ", NI_HAO_MEANINGS, "ni3", "you", true, true);
            assertThat(r.correct()).isTrue();
            assertThat(r.suggestedRating()).isEqualTo(Rating.GOOD);
        }

        @Test
        void caseInsensitiveMeaning() {
            GradingResult r = GradingService.grade("nǐ", NI_HAO_MEANINGS, "ni3", "YOU", true, true);
            assertThat(r.correct()).isTrue();
        }

        @Test
        void trimmedMeaning() {
            GradingResult r = GradingService.grade("nǐ", NI_HAO_MEANINGS, "ni3", "  you  ", true, true);
            assertThat(r.correct()).isTrue();
        }

        @Test
        void anyOfMultipleMeanings() {
            GradingResult r = GradingService.grade("shān", SHAN_MEANINGS,
                    "shan1", "hill", true, true);
            assertThat(r.correct()).isTrue();
        }

        @Test
        void wrongMeaningFails() {
            GradingResult r = GradingService.grade("nǐ", NI_HAO_MEANINGS, "ni3", "river", true, true);
            assertThat(r.correct()).isFalse();
            assertThat(r.nearMatch()).isFalse();
        }
    }

    @Nested
    class MeaningArticleStripping {

        @Test
        void stripsLeadingTo() {
            GradingResult r = GradingService.grade("chī", List.of("to eat"),
                    "chi1", "eat", true, true);
            assertThat(r.correct()).isTrue();
        }

        @Test
        void stripsLeadingThe() {
            GradingResult r = GradingService.grade("guǒ", ARTICLE_MEANINGS,
                    "guo3", "apple", true, true);
            assertThat(r.correct()).isTrue();
        }

        @Test
        void stripsLeadingA() {
            GradingResult r = GradingService.grade("kē", List.of("a tree"),
                    "ke1", "tree", true, true);
            assertThat(r.correct()).isTrue();
        }

        @Test
        void stripsLeadingAn() {
            GradingResult r = GradingService.grade("àn", List.of("an answer"),
                    "an4", "answer", true, true);
            assertThat(r.correct()).isTrue();
        }

        @Test
        void userAlsoUsesArticle() {
            GradingResult r = GradingService.grade("chī", List.of("to eat"),
                    "chi1", "to eat", true, true);
            assertThat(r.correct()).isTrue();
        }

        @Test
        void articleInsideStaysIntact() {
            GradingResult r = GradingService.grade("dú", List.of("read the book"),
                    "du2", "read the book", true, true);
            assertThat(r.correct()).isTrue();
        }
    }

    @Nested
    class MeaningPunctuation {

        @Test
        void edgePunctuationRemoved() {
            GradingResult r = GradingService.grade("nǐ", NI_HAO_MEANINGS, "ni3", "you!", true, true);
            assertThat(r.correct()).isTrue();
        }

        @Test
        void quotesRemoved() {
            GradingResult r = GradingService.grade("nǐ", NI_HAO_MEANINGS, "ni3", "\"you\"", true, true);
            assertThat(r.correct()).isTrue();
        }

        @Test
        void trailingDotRemoved() {
            GradingResult r = GradingService.grade("nǐ", NI_HAO_MEANINGS, "ni3", "you.", true, true);
            assertThat(r.correct()).isTrue();
        }
    }

    @Nested
    class TyposAndNearMatch {

        @Test
        void singleCharTypoIsNearMatch() {
            GradingResult r = GradingService.grade("shān", SHAN_MEANINGS,
                    "shan1", "mauntain", true, true);
            assertThat(r.nearMatch()).isTrue();
            assertThat(r.correct()).isFalse();
            assertThat(r.suggestedRating()).isEqualTo(Rating.HARD);
        }

        @Test
        void missingLetterIsNearMatch() {
            GradingResult r = GradingService.grade("shān", SHAN_MEANINGS,
                    "shan1", "mountan", true, true);
            assertThat(r.nearMatch()).isTrue();
        }

        @Test
        void extraLetterIsNearMatch() {
            GradingResult r = GradingService.grade("shān", SHAN_MEANINGS,
                    "shan1", "mountains", true, true);
            assertThat(r.nearMatch()).isTrue();
        }

        @Test
        void twoCharTypoIsNotNearMatch() {
            GradingResult r = GradingService.grade("shān", SHAN_MEANINGS,
                    "shan1", "muantian", true, true);
            assertThat(r.nearMatch()).isFalse();
            assertThat(r.correct()).isFalse();
        }

        @Test
        void typoToleranceDisabled() {
            GradingResult r = GradingService.grade("shān", SHAN_MEANINGS,
                    "shan1", "mauntain", true, false);
            assertThat(r.correct()).isFalse();
            assertThat(r.nearMatch()).isFalse();
        }

        @Test
        void emptyMeaningIsWrong() {
            GradingResult r = GradingService.grade("nǐ", NI_HAO_MEANINGS, "ni3", "", true, true);
            assertThat(r.correct()).isFalse();
            assertThat(r.nearMatch()).isFalse();
            assertThat(r.suggestedRating()).isEqualTo(Rating.AGAIN);
        }

        @Test
        void nullMeaningIsWrong() {
            GradingResult r = GradingService.grade("nǐ", NI_HAO_MEANINGS, "ni3", null, true, true);
            assertThat(r.correct()).isFalse();
        }

        @Test
        void blankMeaningIsWrong() {
            GradingResult r = GradingService.grade("nǐ", NI_HAO_MEANINGS, "ni3", "   ", true, true);
            assertThat(r.correct()).isFalse();
        }
    }

    @Nested
    class WrongPinyinShortCircuits {

        @Test
        void wrongPinyinNeverNearMatchOnMeaning() {
            GradingResult r = GradingService.grade("shān", SHAN_MEANINGS,
                    "wrong", "mountain", true, true);
            assertThat(r.correct()).isFalse();
            assertThat(r.nearMatch()).isFalse();
        }
    }

    @Nested
    class LevenshteinUnit {

        @Test
        void identicalStringsZero() {
            assertThat(GradingService.levenshtein("apple", "apple")).isZero();
        }

        @Test
        void singleSubstitutionOne() {
            assertThat(GradingService.levenshtein("apple", "apxle")).isEqualTo(1);
        }

        @Test
        void singleInsertionOne() {
            assertThat(GradingService.levenshtein("apple", "apples")).isEqualTo(1);
        }

        @Test
        void singleDeletionOne() {
            assertThat(GradingService.levenshtein("apples", "apple")).isEqualTo(1);
        }

        @Test
        void emptyVsNonEmpty() {
            assertThat(GradingService.levenshtein("", "abc")).isEqualTo(3);
            assertThat(GradingService.levenshtein("abc", "")).isEqualTo(3);
        }
    }
}
