package com.dailycode.bot.application.challenge.dto;

import com.dailycode.bot.domain.problem.ProblemDifficulty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@RequiredArgsConstructor
public class DailyAssignmentDto {

    private final Long id;
    private final Long challengeId;
    private final Long problemId;
    private final LocalDate assignmentDate;
    private final long dayNumber;
    private final String title;
    private final String titleSlug;
    private final ProblemDifficulty difficulty;
    private final Instant deadline;
    private final Instant assignedAt;
    private final boolean created;
}
