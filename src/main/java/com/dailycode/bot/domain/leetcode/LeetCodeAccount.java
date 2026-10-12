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
