package dev.kekao.deck;

import java.util.List;

public final class DeckDtos {

    private DeckDtos() {}

    public record DeckView(
            Long id,
            String name,
            String slug,
            String description,
            boolean isSystem,
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
}
