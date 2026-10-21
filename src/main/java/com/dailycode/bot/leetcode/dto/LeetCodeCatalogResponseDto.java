package com.dailycode.bot.leetcode.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LeetCodeCatalogResponseDto(Data data, List<GraphQlErrorDto> errors) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(Page problemsetQuestionList) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Page(Integer total, List<Question> questions) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Question(String titleSlug, String difficulty, Boolean isPaidOnly) {}
}
