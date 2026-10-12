package com.dailycode.bot.leetcode.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphQlRequest {

    private String operationName;
    private String query;
    private Map<String, Object> variables;
}
