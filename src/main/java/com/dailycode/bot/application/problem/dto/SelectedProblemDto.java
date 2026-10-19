package com.dailycode.bot.application.problem.dto;

import com.dailycode.bot.domain.problem.ProblemDifficulty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SelectedProblemDto {

    private final Long id;
    private final String frontendId;
    private final String title;
    private final String titleSlug;
    private final ProblemDifficulty difficulty;
}
