package dev.kekao.deck;

import dev.kekao.deck.DeckDtos.DeckListResponse;
import dev.kekao.deck.DeckDtos.SubscribeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/decks")
public class DeckController {

    private final DeckService service;

    @Autowired
    public DeckController(DeckService service) {
        this.service = service;
    }

    @GetMapping
    public DeckListResponse list(@AuthenticationPrincipal Jwt jwt) {
        long userId = Long.parseLong(jwt.getSubject());
        return new DeckListResponse(service.listSystemDecks(userId));
    }

    @PostMapping("/{id}/subscribe")
    public ResponseEntity<SubscribeResponse> subscribe(@PathVariable Long id,
                                                       @AuthenticationPrincipal Jwt jwt) {
        long userId = Long.parseLong(jwt.getSubject());
        try {
            return ResponseEntity.ok(service.subscribe(userId, id));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
