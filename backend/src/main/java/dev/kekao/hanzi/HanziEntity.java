package dev.kekao.hanzi;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "hanzi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HanziEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String character;

    @Column(nullable = false, length = 64)
    private String pinyin;

    @Column(name = "stroke_count")
    private Short strokeCount;

    @Column(name = "hsk_level")
    private Short hskLevel;

    @Column(name = "frequency_rank")
    private Integer frequencyRank;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private HanziStatus status;

    @Column(name = "has_stroke_data", nullable = false)
    private boolean hasStrokeData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = HanziStatus.DRAFT;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
