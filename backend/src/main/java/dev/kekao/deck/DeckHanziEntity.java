package dev.kekao.deck;

import dev.kekao.hanzi.HanziEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "deck_hanzi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeckHanziEntity {

    @EmbeddedId
    private DeckHanziId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("deckId")
    @JoinColumn(name = "deck_id")
    private DeckEntity deck;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("hanziId")
    @JoinColumn(name = "hanzi_id")
    private HanziEntity hanzi;

    @Column(nullable = false)
    private Integer position;
}
