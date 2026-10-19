package com.dailycode.bot.application.problem.dto;

import com.dailycode.bot.application.problem.ProblemSyncStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ProblemSyncResultDto {

    private final ProblemSyncStatus status;
    // Absent when a new premium problem is skipped.
    private final Long problemId;
    private final String titleSlug;
}
