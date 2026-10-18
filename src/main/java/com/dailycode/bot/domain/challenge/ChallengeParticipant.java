package com.dailycode.bot.domain.challenge;

import com.dailycode.bot.domain.user.DiscordUser;
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
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
@Table(name = "challenge_participants", uniqueConstraints = @UniqueConstraint(
        name = "uk_challenge_participants_challenge_user", columnNames = {"challenge_id", "discord_user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChallengeParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false, updatable = false)
    private Challenge challenge;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discord_user_id", nullable = false, updatable = false)
    private DiscordUser discordUser;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    public ChallengeParticipant(Challenge challenge, DiscordUser discordUser) {
        this.challenge = Objects.requireNonNull(challenge, "Challenge is required.");
        this.discordUser = Objects.requireNonNull(discordUser, "Discord user is required.");
    }

    @PrePersist
    private void onCreate() {
        joinedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
