package com.dailycode.bot.application.challenge;

import lombok.Getter;

@Getter
public class ChallengeTodayException extends RuntimeException {

    private final Reason reason;

    public ChallengeTodayException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public enum Reason {
        CHALLENGE_NOT_FOUND,
        CHALLENGE_CANCELLED,
        CHALLENGE_COMPLETED,
        CHALLENGE_NOT_STARTED,
        CHALLENGE_ENDED,
        NO_ASSIGNMENT
    }
}
