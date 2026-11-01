package com.dailycode.bot.application.challenge.dto;

import com.dailycode.bot.domain.challenge.ChallengeStatus;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
public class ChallengeDto {

    private final Long id;
    private final String discordGuildId;
    private final String name;
    private final String creatorDiscordUserId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String timezone;
    private final ChallengeStatus status;
    private final Instant createdAt;
    private final ProblemDifficulty difficulty;

    public ChallengeDto(Long id, String discordGuildId, String name, String creatorDiscordUserId,
                        LocalDate startDate, LocalDate endDate, String timezone,
                        ChallengeStatus status, Instant createdAt) {
        this(id, discordGuildId, name, creatorDiscordUserId, startDate, endDate, timezone, status, createdAt, ProblemDifficulty.EASY);
    }

    public ChallengeDto(Long id, String discordGuildId, String name, String creatorDiscordUserId,
                        LocalDate startDate, LocalDate endDate, String timezone,
                        ChallengeStatus status, Instant createdAt, ProblemDifficulty difficulty) {
        this.id = id;
        this.discordGuildId = discordGuildId;
        this.name = name;
        this.creatorDiscordUserId = creatorDiscordUserId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.timezone = timezone;
        this.status = status;
        this.createdAt = createdAt;
        this.difficulty = difficulty == null ? ProblemDifficulty.EASY : difficulty;
    }
}
