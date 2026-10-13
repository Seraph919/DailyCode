package com.dailycode.bot.domain.leetcode;

import com.dailycode.bot.domain.user.DiscordUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "leetcode_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeetCodeAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discord_user_id", nullable = false, unique = true, updatable = false)
    private DiscordUser discordUser;

    @NotBlank
    @Size(max = 100)
    @Column(name = "leetcode_username", nullable = false, length = 100)
    private String leetcodeUsername;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Size(max = 100)
    @Column(name = "verification_token", length = 100)
    private String verificationToken;

    @Column(name = "verification_expires_at")
    private Instant verificationExpiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "last_submission_check_at")
    private Instant lastSubmissionCheckAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public LeetCodeAccount(DiscordUser discordUser, String leetcodeUsername) {
        this.discordUser = discordUser;
        this.leetcodeUsername = leetcodeUsername;
        this.verified = false;
    }

    public void startVerification(String token, Instant expiresAt, Instant now) {
        if (verified) {
            throw new IllegalStateException("Account is already verified.");
        }
        if (token == null || token.isBlank() || token.length() > 100) {
            throw new IllegalArgumentException("Verification token must contain 1 to 100 characters and not be blank.");
        }
        if (expiresAt == null || now == null) {
            throw new IllegalArgumentException("Expiry and current time are required.");
        }

        // Match PostgreSQL's timestamp precision before checking the deadline.
        Instant deadline = expiresAt.truncatedTo(ChronoUnit.MICROS);
        if (!deadline.isAfter(now)) {
            throw new IllegalArgumentException("Verification expiry must be in the future.");
        }

        verificationToken = token;
        verificationExpiresAt = deadline;
    }

    public boolean hasActiveVerificationChallenge(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("Current time is required.");
        }
        if (verified) {
            return false;
        }
        if (verificationToken == null || verificationToken.isBlank() || verificationExpiresAt == null) {
            return false;
        }
        return verificationExpiresAt.isAfter(now);
    }

    public void completeVerification(String expectedToken, Instant now) {
        if (expectedToken == null || expectedToken.isBlank()) {
            throw new IllegalArgumentException("The checked verification token is required.");
        }
        if (!hasActiveVerificationChallenge(now)) {
            throw new IllegalStateException("Account has no active verification challenge.");
        }
        if (!verificationToken.equals(expectedToken)) {
            throw new IllegalStateException("The verification challenge has changed.");
        }

        verified = true;
        verifiedAt = now.truncatedTo(ChronoUnit.MICROS);
        verificationToken = null;
        verificationExpiresAt = null;
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
