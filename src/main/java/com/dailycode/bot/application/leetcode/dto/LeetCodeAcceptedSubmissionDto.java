package com.dailycode.bot.application.leetcode.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class LeetCodeAcceptedSubmissionDto {
    private final String submissionId;
    private final String title;
    private final String titleSlug;
    private final Instant submittedAt;
}
