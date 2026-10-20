package com.dailycode.bot.domain.challenge;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
@Table(name = "daily_progress", uniqueConstraints = @UniqueConstraint(
        name = "uk_daily_progress_participant_day", columnNames = {"participant_id", "daily_challenge_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Shared by both composite foreign keys, enforcing that membership and assignment match.
    @NotNull
    @Column(name = "challenge_id", nullable = false, updatable = false)
    private Long challengeId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false, updatable = false)
    private ChallengeParticipant participant;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_challenge_id", nullable = false, updatable = false)
    private DailyChallenge dailyChallenge;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DailyProgressStatus status = DailyProgressStatus.PENDING;

    @Column(name = "submission_id", length = 50)
    private String submissionId;

    // Time of the accepted submission, distinct from the time the bot verified it.
    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public DailyProgress(ChallengeParticipant participant, DailyChallenge dailyChallenge) {
        Objects.requireNonNull(participant, "Participant is required.");
        Objects.requireNonNull(dailyChallenge, "Daily assignment is required.");
        Long participantChallengeId = participant.getChallenge().getId();
        if (participant.getId() == null || dailyChallenge.getId() == null || participantChallengeId == null) {
            throw new IllegalArgumentException("Persist the participant and daily assignment before recording progress.");
        }
        if (!participantChallengeId.equals(dailyChallenge.getChallenge().getId())) {
            throw new IllegalArgumentException("Participant and daily assignment must belong to the same challenge.");
        }
        this.challengeId = participantChallengeId;
        this.participant = participant;
        this.dailyChallenge = dailyChallenge;
    }

    // Caller must first validate submission ownership, acceptance, problem and eligibility window.
    public void complete(String submissionId, Instant submittedAt, Instant checkedAt) {
        if (submissionId == null || submissionId.isBlank() || submissionId.length() > 50) {
            throw new IllegalArgumentException("Submission ID must contain 1 to 50 nonblank characters.");
        }
        Instant completed = Objects.requireNonNull(submittedAt, "Submission time is required.")
                .truncatedTo(ChronoUnit.MICROS);
        Instant verified = Objects.requireNonNull(checkedAt, "Verification time is required.")
                .truncatedTo(ChronoUnit.MICROS);
        if (verified.isBefore(completed)) {
            throw new IllegalArgumentException("Verification cannot precede the submission.");
        }
        if (status == DailyProgressStatus.COMPLETED) {
            if (this.submissionId.equals(submissionId) && completedAt.equals(completed)) {
                return; // Rechecking the same evidence preserves the first verification time.
            }
            throw new IllegalStateException("Daily progress is already completed with different evidence.");
        }
        this.submissionId = submissionId;
        completedAt = completed;
        verifiedAt = verified;
        status = DailyProgressStatus.COMPLETED;
    }

    @PrePersist
    private void onCreate() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
