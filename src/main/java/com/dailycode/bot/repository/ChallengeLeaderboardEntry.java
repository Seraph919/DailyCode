package com.dailycode.bot.repository;

public interface ChallengeLeaderboardEntry {
    String getDiscordUserId();
    long getCompleted();
    long getRank();
}
