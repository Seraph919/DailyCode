package com.dailycode.bot.application.challenge.dto;

import java.util.List;

public record ChallengeLeaderboardDto(Long challengeId, String name, int page, int totalPages,
                                      long participants, List<Entry> entries) {
    public record Entry(long rank, String discordUserId, long completed) {}
}
