package com.dailycode.bot.application.leetcode.dto;

import com.dailycode.bot.domain.problem.ProblemDifficulty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LeetCodeProblemDto {

    private final String leetCodeQuestionId;
    private final String frontendId;
    private final String title;
    private final String titleSlug;
    private final ProblemDifficulty difficulty;
    private final boolean paidOnly;
}
