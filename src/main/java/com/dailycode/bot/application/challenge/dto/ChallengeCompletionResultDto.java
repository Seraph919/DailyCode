package com.dailycode.bot.application.challenge.dto;

public record ChallengeCompletionResultDto(Long challengeId, int checked, int newlyCompleted,
                                            int alreadyCompleted, int noMatch) {}
