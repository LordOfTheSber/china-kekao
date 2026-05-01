package dev.kekao.study;

import dev.kekao.hanzi.HanziEntity;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "user_card",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "hanzi_id", "mode"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hanzi_id", nullable = false)
    private HanziEntity hanzi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StudyMode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CardState state;

    @Column(nullable = false)
    private double stability;

    @Column(nullable = false)
    private double difficulty;

    @Column(name = "last_review")
    private Instant lastReview;

    @Column(name = "due_date", nullable = false)
    private Instant dueDate;

    @Column(nullable = false)
    private int reps;

    @Column(nullable = false)
    private int lapses;

    @Column(name = "elapsed_days", nullable = false)
    private double elapsedDays;

    @Column(name = "scheduled_days", nullable = false)
    private double scheduledDays;

    @Column(name = "algorithm_version", nullable = false, length = 16)
    private String algorithmVersion;

    @PrePersist
    void onCreate() {
        if (state == null) state = CardState.NEW;
        if (dueDate == null) dueDate = Instant.now();
        if (algorithmVersion == null) algorithmVersion = "FSRS-5";
    }
}
