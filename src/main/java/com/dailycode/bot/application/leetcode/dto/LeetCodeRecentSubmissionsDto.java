package com.dailycode.bot.application.leetcode.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class LeetCodeRecentSubmissionsDto {
    private final String username;
    private final List<LeetCodeAcceptedSubmissionDto> submissions;

    public LeetCodeRecentSubmissionsDto(String username, List<LeetCodeAcceptedSubmissionDto> submissions) {
        this.username = username;
        this.submissions = List.copyOf(submissions);
    }
}
