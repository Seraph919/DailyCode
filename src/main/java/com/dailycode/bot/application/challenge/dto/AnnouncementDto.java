package com.dailycode.bot.application.challenge.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AnnouncementDto {
    private final String challengeName;
    private final String guildId;
    private final String channelId;
    private final String roleId;
    private final DailyAssignmentDto assignment;
    private final String frontendId;
    private final long participantCount;
}
