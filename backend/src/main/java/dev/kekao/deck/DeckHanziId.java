package dev.kekao.deck;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DeckHanziId implements Serializable {

    @Column(name = "deck_id")
    private Long deckId;

    @Column(name = "hanzi_id")
    private Long hanziId;
}
