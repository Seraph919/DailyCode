package com.dailycode.bot.leetcode.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeetCodeSubmissionDto {
    private String id;
    private String title;
    private String titleSlug;
    private String timestamp;
}
