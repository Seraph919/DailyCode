package com.dailycode.bot.leetcode.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeetCodeProfileResponse {

    private LeetCodeProfileData data;
    private List<GraphQlError> errors;
}
