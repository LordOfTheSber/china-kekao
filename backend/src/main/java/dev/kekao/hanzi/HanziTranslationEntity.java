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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "hanzi_translation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"hanzi_id", "language"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HanziTranslationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hanzi_id", nullable = false)
    private HanziEntity hanzi;

    @Column(nullable = false, length = 8)
    private String language;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "meanings", nullable = false, columnDefinition = "text[]")
    private List<String> meanings;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;
}
