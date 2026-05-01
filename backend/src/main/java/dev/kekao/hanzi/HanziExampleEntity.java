package dev.kekao.hanzi;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hanzi_example")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HanziExampleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hanzi_id", nullable = false)
    private HanziEntity hanzi;

    @Column(nullable = false, length = 8)
    private String language;

    @Column(nullable = false, columnDefinition = "text")
    private String sentence;

    @Column(nullable = false, columnDefinition = "text")
    private String pinyin;

    @Column(nullable = false, columnDefinition = "text")
    private String translation;
}
