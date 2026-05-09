package dev.kekao.deck;

import dev.kekao.deck.DeckDtos.AddHanziRequest;
import dev.kekao.deck.DeckDtos.CreateDeckRequest;
import dev.kekao.deck.DeckDtos.DeckDetailView;
import dev.kekao.deck.DeckDtos.DeckListResponse;
import dev.kekao.deck.DeckDtos.DeckView;
import dev.kekao.deck.DeckDtos.SubscribeResponse;
import dev.kekao.deck.DeckDtos.UpdateDeckRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
        return new DeckListResponse(service.listDecks(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeckDetailView> get(@PathVariable Long id,
                                              @AuthenticationPrincipal Jwt jwt) {
        long userId = Long.parseLong(jwt.getSubject());
        try {
            return ResponseEntity.ok(service.getDeck(userId, id));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<DeckView> create(@Valid @RequestBody CreateDeckRequest request,
                                           @AuthenticationPrincipal Jwt jwt) {
        long userId = Long.parseLong(jwt.getSubject());
        DeckView created = service.createDeck(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DeckView> update(@PathVariable Long id,
                                           @Valid @RequestBody UpdateDeckRequest request,
                                           @AuthenticationPrincipal Jwt jwt) {
        long userId = Long.parseLong(jwt.getSubject());
        try {
            return ResponseEntity.ok(service.updateDeck(userId, id, request));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.notFound().build();
        } catch (DeckService.AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal Jwt jwt) {
        long userId = Long.parseLong(jwt.getSubject());
        try {
            service.deleteDeck(userId, id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException ex) {
            return ResponseEntity.notFound().build();
        } catch (DeckService.AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping("/{id}/hanzi")
    public ResponseEntity<DeckDetailView> addHanzi(@PathVariable Long id,
                                                   @Valid @RequestBody AddHanziRequest request,
                                                   @AuthenticationPrincipal Jwt jwt) {
        long userId = Long.parseLong(jwt.getSubject());
        try {
            return ResponseEntity.ok(service.addHanzi(userId, id, request.hanziIds()));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.notFound().build();
        } catch (DeckService.AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @DeleteMapping("/{id}/hanzi/{hanziId}")
    public ResponseEntity<Void> removeHanzi(@PathVariable Long id,
                                            @PathVariable Long hanziId,
                                            @AuthenticationPrincipal Jwt jwt) {
        long userId = Long.parseLong(jwt.getSubject());
        try {
            service.removeHanzi(userId, id, hanziId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException ex) {
            return ResponseEntity.notFound().build();
        } catch (DeckService.AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
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
