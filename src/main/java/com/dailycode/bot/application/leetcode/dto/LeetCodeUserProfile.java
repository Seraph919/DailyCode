package com.dailycode.bot.application.leetcode.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeetCodeUserProfile {

    private String username;
    private Integer ranking;
}
