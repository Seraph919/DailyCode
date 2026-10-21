package com.dailycode.bot.application.challenge.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import com.dailycode.bot.domain.problem.ProblemDifficulty;

@Getter
@RequiredArgsConstructor
public class AutomationTargetDto {
    private final Long challengeId;
    private final String guildId;
    private final String channelId;
    private final ProblemDifficulty difficulty;
}
