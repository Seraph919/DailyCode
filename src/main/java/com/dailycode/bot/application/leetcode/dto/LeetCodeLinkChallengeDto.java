package com.dailycode.bot.application.leetcode.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class LeetCodeLinkChallengeDto {

    private final String leetcodeUsername;
    private final String verificationToken;
    private final Instant expiresAt;
}
