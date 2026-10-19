package com.dailycode.bot.application.challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ChallengeParticipantDto {

    private final Long id;
    private final Long challengeId;
    private final String challengeName;
    private final String discordGuildId;
    private final String discordUserId;
    private final Instant joinedAt;
}
