package dev.kekao.hanzi;

import java.util.List;

public final class HanziDtos {

    private HanziDtos() {}

    public record HanziSummary(
            Long id,
            String character,
            String pinyin,
            Short strokeCount,
            Short hskLevel,
            Integer frequencyRank,
            List<String> meaningsEn,
            boolean hasStrokeData
    ) {}

    public record HanziSearchPage(
            List<HanziSummary> items,
            int page,
            int size,
            long total
    ) {}

    public record HanziExampleView(
            String sentence,
            String pinyin,
            String translation
    ) {}

    public record UserCardSummary(
            Long userCardId,
            String mode,
            String state,
            String dueDate
    ) {}

    public record HanziDetailView(
            Long id,
            String character,
            String pinyin,
            Short strokeCount,
            Short hskLevel,
            Integer frequencyRank,
            List<String> meaningsEn,
            boolean hasStrokeData,
            List<HanziExampleView> examples,
            List<UserCardSummary> userCards
    ) {}
}
