package dev.kekao.study;

import dev.kekao.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "review_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_card_id", nullable = false)
    private UserCardEntity userCard;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private short rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "state_before", nullable = false, length = 16)
    private CardState stateBefore;

    @Column(name = "elapsed_days", nullable = false)
    private double elapsedDays;

    @Column(name = "scheduled_days", nullable = false)
    private double scheduledDays;

    @Column(name = "stability_before", nullable = false)
    private double stabilityBefore;

    @Column(name = "difficulty_before", nullable = false)
    private double difficultyBefore;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "hint_count", nullable = false)
    private short hintCount;

    @Column(name = "stroke_mistakes", nullable = false)
    private short strokeMistakes;

    @PrePersist
    void onCreate() {
        if (reviewedAt == null) reviewedAt = Instant.now();
    }
}
