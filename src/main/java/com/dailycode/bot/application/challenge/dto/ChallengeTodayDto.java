package com.dailycode.bot.application.challenge.dto;

import com.dailycode.bot.domain.problem.ProblemDifficulty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@RequiredArgsConstructor
public class ChallengeTodayDto {

    private final Long challengeId;
    private final String challengeName;
    private final long dayNumber;
    private final LocalDate date;
    private final String timezone;
    private final String title;
    private final String titleSlug;
    private final ProblemDifficulty difficulty;
    private final Instant deadline;
}
