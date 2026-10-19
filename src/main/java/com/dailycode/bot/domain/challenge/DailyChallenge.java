package com.dailycode.bot.domain.challenge;

import com.dailycode.bot.domain.problem.Problem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
@Table(name = "daily_challenges", uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_challenges_day", columnNames = {"challenge_id", "assignment_date"}),
        @UniqueConstraint(name = "uk_daily_challenges_problem", columnNames = {"challenge_id", "problem_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false, updatable = false)
    private Challenge challenge;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false, updatable = false)
    private Problem problem;

    @NotNull
    @Column(name = "assignment_date", nullable = false, updatable = false)
    private LocalDate assignmentDate;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    public DailyChallenge(Challenge challenge, Problem problem, LocalDate assignmentDate) {
        this.challenge = Objects.requireNonNull(challenge, "Challenge is required.");
        this.problem = Objects.requireNonNull(problem, "Problem is required.");
        this.assignmentDate = Objects.requireNonNull(assignmentDate, "Assignment date is required.");
        if (assignmentDate.isBefore(challenge.getStartDate()) || assignmentDate.isAfter(challenge.getEndDate())) {
            throw new IllegalArgumentException("Assignment date must fall within the challenge dates.");
        }
    }

    @PrePersist
    private void onCreate() {
        assignedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
