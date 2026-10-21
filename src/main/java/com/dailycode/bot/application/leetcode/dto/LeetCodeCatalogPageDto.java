package com.dailycode.bot.application.leetcode.dto;

import java.util.List;

public record LeetCodeCatalogPageDto(int total, List<Entry> entries) {
    public LeetCodeCatalogPageDto {
        entries = List.copyOf(entries);
    }

    public record Entry(String titleSlug, boolean paidOnly) {}
}
