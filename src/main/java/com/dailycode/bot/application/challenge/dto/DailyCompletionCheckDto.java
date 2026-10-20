package com.dailycode.bot.application.challenge.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

// Detached preflight snapshot; no database lock is held while fetching submissions.
@Getter
@RequiredArgsConstructor
public class DailyCompletionCheckDto {
    private final Long challengeId;
    private final String guildId;
    private final String discordUserId;
    private final Long participantId;
    private final Long assignmentId;
    private final LocalDate assignmentDate;
    private final Long problemId;
    private final String titleSlug;
    private final Long accountId;
    private final String username;
    private final Instant accountVerifiedAt;
    private final Instant eligibleFrom;
    private final Instant deadline;
    private final DailyCompletionResultDto existingCompletion;
}
