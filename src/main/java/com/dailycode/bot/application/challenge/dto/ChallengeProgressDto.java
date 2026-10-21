package com.dailycode.bot.application.challenge.dto;

import java.time.LocalDate;
import java.util.List;

public record ChallengeProgressDto(Long challengeId, String name, String timezone,
        LocalDate today, String todayStatus, boolean automationEnabled, long completed,
        int page, int totalPages, List<Day> history) {
    public record Day(LocalDate date, String title, String status) {}
}
