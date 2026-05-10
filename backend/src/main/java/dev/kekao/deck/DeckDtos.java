package dev.kekao.deck;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class DeckDtos {

    private DeckDtos() {}

    public record DeckView(
            Long id,
            String name,
            String slug,
            String description,
            boolean isSystem,
            boolean owned,
            int hanziCount,
            boolean subscribed
    ) {}

    public record DeckListResponse(List<DeckView> decks) {}

    public record SubscribeResponse(
            Long deckId,
            int newlyCreatedCards,
            int totalCardsInDeck,
            boolean alreadySubscribed
    ) {}

    public record CreateDeckRequest(
            @NotBlank @Size(max = 128) String name,
            @Size(max = 4000) String description
    ) {}

    public record UpdateDeckRequest(
            @NotBlank @Size(max = 128) String name,
            @Size(max = 4000) String description
    ) {}

    public record AddHanziRequest(
            @NotNull List<@NotNull Long> hanziIds
    ) {}

    public record DeckHanziView(
            Long hanziId,
            String character,
            String pinyin,
            Short hskLevel,
            Integer position,
            List<String> meaningsEn
    ) {}

    public record DeckDetailView(
            Long id,
            String name,
            String slug,
            String description,
            boolean isSystem,
            boolean owned,
            boolean subscribed,
            int hanziCount,
            List<DeckHanziView> entries
    ) {}
}
