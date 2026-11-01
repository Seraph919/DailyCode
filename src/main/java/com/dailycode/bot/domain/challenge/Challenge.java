package com.dailycode.bot.domain.challenge;

import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
@Table(name = "challenges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 20)
    @Column(name = "discord_guild_id", nullable = false, length = 20, updatable = false)
    private String discordGuildId;

    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_discord_user_id", nullable = false, updatable = false)
    private DiscordUser createdBy;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotBlank
    @Size(max = 100)
    @Column(name = "timezone", nullable = false, length = 100)
    private String timezone;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChallengeStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "announcement_channel_id", length = 20)
    private String announcementChannelId;

    @Column(name = "announcement_role_id", length = 20)
    private String announcementRoleId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 10)
    private ProblemDifficulty difficulty = ProblemDifficulty.EASY;

    @NotNull
    @Column(name = "automation_time", nullable = false)
    private LocalTime automationTime = LocalTime.MIDNIGHT;

    @NotNull
    @Column(name = "automation_frequency", nullable = false)
    private int automationFrequency = 7;

    public ProblemDifficulty getDifficulty() {
        return difficulty;
    }

    public ProblemDifficulty getAutomationDifficulty() {
        return difficulty;
    }

    public int getAutomationFrequency() {
        return automationFrequency;
    }

    public void automate(String channelId, ProblemDifficulty difficulty, LocalTime time, String roleId) {
        automate(channelId, difficulty, time, roleId, 7);
    }

    public void automate(String channelId, ProblemDifficulty difficulty, LocalTime time, String roleId, Integer frequency) {
        if (channelId == null || !channelId.matches("[0-9]{1,20}")) {
            throw new IllegalArgumentException("A Discord channel ID is required.");
        }
        Objects.requireNonNull(difficulty, "Difficulty is required.");
        Objects.requireNonNull(time, "Daily posting time is required.");
        if (time.getSecond() != 0 || time.getNano() != 0) {
            throw new IllegalArgumentException("Daily posting time must use whole minutes.");
        }
        if (roleId != null && !roleId.matches("[0-9]{1,20}")) {
            throw new IllegalArgumentException("A valid Discord role ID is required.");
        }
        int freq = frequency == null ? 7 : frequency;
        if (freq < 1 || freq > 7) {
            throw new IllegalArgumentException("Posting frequency must be between 1 and 7 days per week.");
        }
        announcementChannelId = channelId;
        announcementRoleId = roleId;
        this.difficulty = difficulty;
        automationTime = time;
        automationFrequency = freq;
    }

    public boolean isAutomationScheduledOn(LocalDate date) {
        if (date == null) {
            return false;
        }
        return isAutomationScheduledFor(date.getDayOfWeek());
    }

    public boolean isAutomationScheduledFor(DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) {
            return false;
        }
        return switch (automationFrequency) {
            case 1 -> dayOfWeek == DayOfWeek.MONDAY;
            case 2 -> dayOfWeek == DayOfWeek.TUESDAY || dayOfWeek == DayOfWeek.THURSDAY;
            case 3 -> dayOfWeek == DayOfWeek.MONDAY || dayOfWeek == DayOfWeek.WEDNESDAY || dayOfWeek == DayOfWeek.FRIDAY;
            case 4 -> dayOfWeek == DayOfWeek.MONDAY || dayOfWeek == DayOfWeek.TUESDAY || dayOfWeek == DayOfWeek.THURSDAY || dayOfWeek == DayOfWeek.FRIDAY;
            case 5 -> dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
            case 6 -> dayOfWeek != DayOfWeek.SUNDAY;
            default -> true;
        };
    }

    public void stopAutomation() {
        announcementChannelId = null;
    }

    public Challenge(String discordGuildId, String name, DiscordUser createdBy,
                     LocalDate startDate, LocalDate endDate, String timezone) {
        this(discordGuildId, name, createdBy, startDate, endDate, timezone, ProblemDifficulty.EASY);
    }

    public Challenge(String discordGuildId, String name, DiscordUser createdBy,
                     LocalDate startDate, LocalDate endDate, String timezone, ProblemDifficulty difficulty) {
        this.discordGuildId = requireText(discordGuildId, "Discord guild ID", 20);
        this.name = requireText(name, "Challenge name", 100);
        this.createdBy = Objects.requireNonNull(createdBy, "Challenge creator is required.");
        this.startDate = Objects.requireNonNull(startDate, "Start date is required.");
        this.endDate = Objects.requireNonNull(endDate, "End date is required.");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must not be before start date.");
        }
        // Dates include both endpoints and refer to calendar days in this zone.
        this.timezone = ZoneId.of(requireText(timezone, "Timezone", 100)).getId();
        this.status = ChallengeStatus.SCHEDULED;
        this.difficulty = difficulty == null ? ProblemDifficulty.EASY : difficulty;
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank() || value.strip().length() > maxLength) {
            throw new IllegalArgumentException(field + " must contain 1 to " + maxLength + " characters.");
        }
        return value.strip();
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
