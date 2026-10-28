package com.dailycode.bot.application.challenge.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AutomaticCheckTargetDto {
    private final Long challengeId;
    private final String guildId;
    private final String discordUserId;
}
