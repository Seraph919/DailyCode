package com.dailycode.bot.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "discord_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiscordUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 20)
    @Column(name = "discord_user_id", nullable = false, unique = true, length = 20, updatable = false)
    private String discordUserId;

    @NotBlank
    @Size(max = 100)
    @Column(name = "discord_username", nullable = false, length = 100)
    private String discordUsername;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DiscordUser(String discordUserId, String discordUsername) {
        this.discordUserId = discordUserId;
        this.discordUsername = discordUsername;
    }

    public void changeUsername(String discordUsername) {
        this.discordUsername = discordUsername;
    }

    @PrePersist
    private void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
    }

}
