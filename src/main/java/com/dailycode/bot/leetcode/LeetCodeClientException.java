package com.dailycode.bot.leetcode;

public class LeetCodeClientException extends RuntimeException {

    public LeetCodeClientException(String message) {
        super(message);
    }

    public LeetCodeClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
