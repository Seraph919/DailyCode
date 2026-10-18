package com.dailycode.bot.application.challenge.dto;

import com.dailycode.bot.domain.challenge.ChallengeStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
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
}
