package com.dailycode.bot.application.challenge.dto;

import com.dailycode.bot.application.challenge.DailyCompletionOutcome;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@RequiredArgsConstructor
public class DailyCompletionResultDto {
    private final Long challengeId;
    private final Long assignmentId;
    private final LocalDate assignmentDate;
    private final Long progressId;
    private final DailyCompletionOutcome outcome;
    private final String submissionId;
    private final Instant completedAt;
}
